#/bin/bash
rm -f /home/tom/tcu.log
rm -f /home/tom/tcu_error.log
java -jar /home/tom/tcu.jar 1>/home/tom/tcu.log 2>/home/tom/tcu_error.log

