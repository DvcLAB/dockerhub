#! /bin/bash
pkill fp-multiuser && nohup /opt/frps/fp-multiuser -l 127.0.0.1:7200 -f /opt/frps/tokens > nohup.out 2> nohup.err < /dev/null &