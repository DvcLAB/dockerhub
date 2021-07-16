#! /bin/bash
pkill fp-multiuser && nohup /opt/frps/fp-multiuser -l 172.17.221.237:7200 -f /opt/frps/tokens > nohup.out 2> nohup.err < /dev/null &