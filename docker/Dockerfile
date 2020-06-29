FROM openjdk:8-stretch

MAINTAINER Nwanya Justin "justin.nwanya@mintfintech.com"

EXPOSE 8096

WORKDIR /usr/local/bin/

COPY savings-service.jar savings-service.jar

ENV TZ=Africa/Lagos

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ENTRYPOINT [ "sh", "-c", "java -jar savings-service.jar"]
