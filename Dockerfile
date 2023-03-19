FROM bczhc/wolfram:13.2.0

WORKDIR /home/wolframengine

COPY /atimes .
COPY /generate-commits-scatter.wls .
COPY /generate-commits-per-day-hour-barchart.wls .
COPY /generate-images .

CMD ./generate-images
