import sys
import os
import subprocess
import time
import Queue

from async_file_reader import AsynchronousFileReader


class AdbPush():
    '''
    Helper class to push a file to the private files dir of Tribler on Android.
    '''

    def __init__(self, argv, adb):
        self._adb = adb
        nr_args = len(argv)

        if nr_args > 1:
            self._input_file = os.path.realpath(argv[1])
            print ' Input file:', self._input_file
        else:
            print 'Error: No input file specified!'
            exit()

        if not os.path.exists(self._input_file):
            print 'Error: Input file does not exist!'
            exit()

        if os.path.isdir(self._input_file):
            print 'Error: Cannot copy directories!'
            exit()

        file_name = os.path.basename(self._input_file)
        self._temp_file = '/sdcard/' + str(time.time()) + file_name
        print '  Temp file:', self._temp_file

        if nr_args > 2:
            self._output_file = argv[2]
        else:
            self._output_file = file_name

        print 'Output file:', self._output_file


    def run(self):
        # Push file
        cmd_push = self._adb + ' push ' + self._input_file + ' ' + self._temp_file
        print cmd_push
        push = subprocess.Popen(cmd_push.split())
        push.wait()

        # Start reading logcat
        cmd_logcat = self._adb + ' logcat -v time tag long'
        logcat = subprocess.Popen(cmd_logcat.split(), stdout=subprocess.PIPE)

        stdout_queue = Queue.Queue()
        stdout_reader = AsynchronousFileReader(logcat.stdout, stdout_queue)
        stdout_reader.start()

        # Start copy file
        cmd_copy = self._adb + ' shell am start -n org.tribler.android/.CopyFilesActivity --es "' + self._temp_file + '" "' + self._output_file + '"'
        print cmd_copy
        copy = subprocess.Popen(cmd_copy.split())

        started = False

        # Read until nothing more to read
        while not stdout_reader.eof():
            time.sleep(0.1)
            while not stdout_queue.empty():
                line = stdout_queue.get().strip()

                if ': ' not in line: 
                    break

                device_date, device_time, log = line.split(' ', 2)
                tag, path = log.split(': ', 1)

                if tag.startswith('E/CopyFile'):
                    print log
                    break

                if tag.startswith('I/CopyFileStartIn'):
                    if path == self._temp_file:
                        started = True
                        print log

                    break

                if tag.startswith('I/CopyFileStartOut'):
                    if started and path.endswith(self._output_file):
                        print log

                    break

                if tag.startswith('I/CopyFileDoneIn'):
                    if started:
                        print log

                    break

                if tag.startswith('I/CopyFileDoneOut'):
                    if not started or not path.endswith(self._output_file):
                        break

                    print log

                    # Cleanup
                    cmd_remove = self._adb + ' shell rm "' + self._temp_file + '"'
                    print cmd_remove
                    remove = subprocess.Popen(cmd_remove.split())
                    remove.wait()

                    print 'Finished!'
                    logcat.kill()
                    exit()



if __name__ == '__main__':
    AdbPush(sys.argv, os.getenv('ADB', 'adb')).run()


