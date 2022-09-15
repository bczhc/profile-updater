FROM wolframresearch/wolframengine:13.0.1
WORKDIR /home/wolframengine

COPY /atimes .
COPY /generate-commits-scatter.wls .
COPY /generate-commits-per-day-hour-barchart.wls .
COPY /generate-images .
COPY /login-wolfram-engine .
COPY /credentials .

RUN ./login-wolfram-engine
RUN ./generate-images
