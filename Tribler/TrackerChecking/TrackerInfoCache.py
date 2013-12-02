# ============================================================
# written by Lipu Fei
#
# This module maintains a list of trackers and their info. These information
# is also stored in the database.
#
# It provides two APIs: one is to update a tracker's info, the other is to
# check if a tracker is worth checking now. Because some trackers are gone
# or unreachable by some reason, it wastes a lot of time to check those
# "dead" trackers over and over again.
# ============================================================

import sys
import time
from threading import RLock

from Tribler.Core.Session import Session
from Tribler.Core.CacheDB.Notifier import NTFY_TRACKERINFO, NTFY_INSERT
from Tribler.Core.CacheDB.sqlitecachedb import forceDBThread, forceAndReturnDBThread
from Tribler.Core.CacheDB.CacheDBHandler import TorrentDBHandler

# some default configurations
DEBUG = False

DEFAULT_MAX_TRACKER_FAILURES        = 5 # A tracker that have failed for this
                                        # times will be regarded as "dead"
DEFAULT_DEAD_TRACKER_RETRY_INTERVAL = 60 # A "dead" tracker will be retired
                                         # every 60 seconds

# ============================================================
# This class maintains the tracker infomation cache.
# ============================================================
class TrackerInfoCache(object):

    # ------------------------------------------------------------
    # Initialization.
    # ------------------------------------------------------------
    def __init__(self,\
            max_failures=DEFAULT_MAX_TRACKER_FAILURES,\
            dead_tracker_recheck_interval=60):
        self._torrentdb = TorrentDBHandler.getInstance()
        self._tracker_info_dict = dict()

        self._tracker_update_request_dict = dict()

        self._max_tracker_failures = max_failures
        self._dead_tracker_recheck_Interval = dead_tracker_recheck_interval

        self._lock = RLock()

        session = Session.get_instance()
        session.add_observer(self.newTrackerCallback, NTFY_TRACKERINFO, [NTFY_INSERT,])

    # ------------------------------------------------------------
    # Loads and initializes the cache from database.
    # ------------------------------------------------------------
    @forceAndReturnDBThread
    def loadCacheFromDb(self):
        tracker_info_list = self._torrentdb.getTrackerInfoList()

        # no need to use the lock when reloading
        self._lock.acquire()
        # update tracker info
        if self._tracker_info_dict:
            for tracker_info in tracker_info_list:
                tracker, alive, last_check, failures = tracker_info
                self._tracker_info_dict[tracker] = dict()
                self._tracker_info_dict[tracker]['last_check'] = last_check
                self._tracker_info_dict[tracker]['failures'] = failures
                self._tracker_info_dict[tracker]['alive'] = alive
                self._tracker_info_dict[tracker]['updated'] = False
        self._lock.release()

    # ------------------------------------------------------------
    # The callback function when a new tracker has been inserted.
    # ------------------------------------------------------------
    def newTrackerCallback(self, subject, changeType, objectID, *args):
        if not objectID:
            return

        # create new trackers
        with self._lock:
            # DB upgrade complete, reload everthing from DB
            if not objectID:
                self.loadCacheFromDb()
                return

            # new tracker insertion callback
            for tracker in objectID:
                if DEBUG:
                    print >> sys.stderr, '[D] New tracker[%s].' % tracker
                self._tracker_info_dict[tracker] = dict()
                self._tracker_info_dict[tracker]['last_check'] = 0
                self._tracker_info_dict[tracker]['failures'] = 0
                self._tracker_info_dict[tracker]['alive'] = True
                self._tracker_info_dict[tracker]['updated'] = False

                # check all the pending update requests
                if tracker not in self._tracker_update_request_dict:
                    continue

                for request in self._tracker_update_request_dict[tracker]:
                    if DEBUG:
                        print >> sys.stderr,\
                        '[D] Handling new tracker[%s] request: %s' % request
                    self.updateTrackerInfo(tracker, request)
                del self._tracker_update_request_dict[tracker]

    # ------------------------------------------------------------
    # (Public API)
    # Checks if a tracker is worth checking now.
    # ------------------------------------------------------------
    def toCheckTracker(self, tracker):
        currentTime = int(time.time())

        with self._lock:
            if not tracker in self._tracker_info_dict:
                return True

            alive = self._tracker_info_dict[tracker]['alive']
            if alive:
                return True

            # check the last time we check this 'dead' tracker
            last_check = self._tracker_info_dict[tracker]['last_check']
            interval = currentTime - last_check
            return interval >= self._dead_tracker_recheck_Interval

    # ------------------------------------------------------------
    # (Public API)
    # Updates or a tracker's information. If the tracker does not
    # exist, it will be created.
    # ------------------------------------------------------------
    def updateTrackerInfo(self, tracker, success):
        currentTime = int(time.time())

        self._lock.acquire()
        if tracker in self._tracker_info_dict:
            tracker_info = self._tracker_info_dict[tracker]
        else:
            # put into a request queue and update after the tracker has been
            # added by the DB thread.
            if tracker not in self._tracker_update_request_dict:
                self._tracker_update_request_dict[tracker] = list()
            self._tracker_update_request_dict[tracker].append(success)

            self._lock.release()
            return

        tracker_info['last_check'] = currentTime
        # reset the failures count if successful
        if success:
            tracker_info['failures'] = 0
        else:
            tracker_info['failures'] += 1

        # determine if a tracker is alive
        if tracker_info['failures'] >= self._max_tracker_failures:
            alive = False
        else:
            alive = True
        tracker_info['alive'] = alive

        self._tracker_info_dict[tracker]['updated'] = True
        self._lock.release()

    # ------------------------------------------------------------
    # (Public API)
    # Updates the tracker status into the DB in batch.
    # ------------------------------------------------------------
    @forceDBThread
    def updateTrackerInfoIntoDb(self):
        self._lock.acquire()

        # store all recently updated tracker info into DB
        update_list = list()
        for tracker, info in self._tracker_info_dict.items():
            if not info['updated']:
                continue

            data = (info['last_check'], info['failures'], info['alive'], tracker)
            update_list.append(data)

            info['updated'] = False

        self._torrentdb.updateTrackerInfo(update_list)

        self._lock.release()

    # ------------------------------------------------------------
    # Gets the size of the tracker info list.
    # ------------------------------------------------------------
    def getTrackerInfoListSize(self):
        with self._lock:
            return len(self._tracker_info_dict.keys())

    # ------------------------------------------------------------
    # Gets the a specific tracker info.
    # ------------------------------------------------------------
    def getTrackerInfo(self, index):
        with self._lock:
            return self._tracker_info_dict.items()[index]