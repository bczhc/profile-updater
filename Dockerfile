FROM bczhc/wolfram

WORKDIR /

COPY /atimes /
COPY /generate-commits-scatter.wls /
COPY /generate-commits-per-day-hour-barchart.wls /
COPY /generate-images /

CMD /generate-images
