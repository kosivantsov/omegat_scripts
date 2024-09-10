#!/bin/bash
#####################################################################
# Run OmegaT in Xvfb. Useful to load OmegaT team project in batches #
#####################################################################
# Variables
JAR=/opt/omegat/OmegaT_5.7.1_Beta_Without_JRE/OmegaT.jar
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
REPOCREDENTIALS="$HOME/.omegat/repositories.properties"
OMTCONFIG=/tmp/headlessOmT
PIDFILE=/tmp/xvfbOmt.pid

# Check if the correct project path was passed
PROJECT="$@"
echo "Specified project: $PROJECT"
if [ -z "$PROJECT" ] || [ ! -e "$PROJECT" ] ; then
    echo "The project specified on the command line doesn't exist"
    exit 2
fi

# Pidfile is used to make sure that a previously spanned and still running Xvfb
# is reused
if [ ! -f $PIDFILE ] ; then
  install -D <(echo -n) $PIDFILE
fi
. $PIDFILE

# Check if Xvfb was already started
if ! [ -z "$XVFBPID" ] ; then
  if [ "$(ps -p $XVFBPID -o pid=)" ] ; then
    echo "Xvfb PID $XVFBPID already started and will be reused or stopped"
  else
    echo "Recorded Xvfb PID is wrong, trying to find Xvfb running :5.0"
    sed -i '/^XVFBPID=.*/d' $PIDFILE
    unset XVFBPID
  fi
fi
if [ $(pgrep Xvfb) ] && [ -z "$XVFBPID" ]; then
  for PID in $(pgrep Xvfb) ; do
    if [ "$(ps -p $PID -o command=|grep -o ":[0-9]")" == ":5" ] ; then
      echo "XVFBPID=$PID" >> $PIDFILE
      echo "Found working Xvfb PID $PID"
    fi
  done;
fi

startXvfb(){
    if [ -z "$(pgrep Xvfb)" ] && [ -z $XVFBPID ]; then
      Xvfb :5 -ac -screen 0 800x600x24 &
      echo "XVFBPID=$!" >> $PIDFILE
    fi
}

startOmT(){
    export DISPLAY=:5.0
    unset _JAVA_OPTIONS
    # Create a tmp OmegaT config directory,
    # Create a script that exits OmegaT when the project is successfully loaded,
    # Copy the file with saved repo credentials
    mkdir -p $OMTCONFIG/scripts/project_changed
    cd $OMTCONFIG
    cp $REPOCREDENTIALS ./
    echo """import static org.omegat.core.events.IProjectEventListener.PROJECT_CHANGE_TYPE.*
if (eventType == LOAD) {
    System.exit(0)
}""" > $OMTCONFIG/scripts/project_changed/script.groovy
    $JAVA_HOME/bin/java -jar $JAR --config-dir="$OMTCONFIG" "$PROJECT"
    # This file is going to be used for final cleanup
    echo "Done" > $OMTCONFIG/finished
}

startXvfb
startOmT
while true ; do
    if [ -f "$OMTCONFIG/finished" ] ; then
        kill -15 $XVFBPID
        rm -R "$OMTCONFIG"
        rm $PIDFILE
        exit 0
    else
        sleep 2
    fi
done
exit 0
