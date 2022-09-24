import os
import re
import sys

#
#  read log and identify document id that has appear in more than 1 log files.
#

entries = {}

def scan_log(logfile):
    with open(logfile) as f:
        for line in f:
            parts = line.strip().split("#produced#")
            if len(parts) > 1:
                # print(f"parts = {parts[1]}")
                for id in eval(parts[1]):
                    try:
                        entries[id] += 1
                    except KeyError:
                        entries[id] = 1


if __name__ == "__main__":
    if (len(sys.argv) > 1):
        for log_f in sys.argv[1:]:
            scan_log(log_f)
    
    errors = [ id for id in entries if entries[id] > 1 ]
    print("errors:", errors)
