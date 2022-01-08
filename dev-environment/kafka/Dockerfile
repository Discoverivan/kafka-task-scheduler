FROM alpine:latest
RUN apk add openjdk11
RUN cd ~ && \
    wget https://dlcdn.apache.org/kafka/3.0.0/kafka_2.13-3.0.0.tgz && \
    tar zxvf kafka_2.13-3.0.0.tgz && \
    mv kafka_2.13-3.0.0 kafka && \
    rm kafka_2.13-3.0.0.tgz
RUN cd ~/kafka/bin && \
    sed -i "s/exec/sh/" zookeeper-server-start.sh && \
    sed -i "s/exec/sh/" kafka-server-start.sh
COPY run.sh /root
CMD sh ~/run.sh

EXPOSE 9092
EXPOSE 2181

