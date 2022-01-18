#!/bin/bash
PDFVIEWER=zathura
CURRENTDOC=$1
FILENAME=$2
PROJROOT=$3
PDFDIR="${PROJROOT}pdf"
if [ ! -d "$PDFDIR" ]; then
    mkdir "$PDFDIR"
fi
ln -sf "$CURRENTDOC" "$PDFDIR"
cd $PDFDIR
mv "$(basename "$FILENAME")" "current."${FILENAME##*.}""  
lowriter --convert-to pdf --outdir $PDFDIR $PDFDIR/current.${FILENAME##*.} > /dev/null 2>&1
ps ux|grep zathura|grep current.pdf > /tmp/omt.txt
if [ -z "$(ps ux|grep $PDFVIEWER|grep current.pdf)" ]; then
    $PDFVIEWER $PDFDIR/current.pdf > /dev/null 2>&1 &
    echo "Previewing current target file"
    exit
fi
exit 
