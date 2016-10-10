FROM 448613307115.dkr.ecr.eu-west-1.amazonaws.com/jvm-oracle:latest

ENV JETTY_HOME="/usr/local/jetty" \
    SUN_NET_INETADDR_TTL="60" \
    ATLAS_APIKEY="" \
    ATLAS_HOST="stage.atlas.metabroadcast.com" \
    AWS_ACCESSKEY="" \
    AWS_SECRETKEY="" \
    SERVER_PORT="8080" \
    JSSE_ENABLESNIEXTENSION="false" \
    LOG4J_CONFIGURATION="file:////usr/local/jetty/log4j.properties" \
    NIMROD_LOG_LEVEL="INFO" \
    ROOT_LOG_LEVEL="INFO" \
    JVM_MEMORY="64M" \
    JVM_OPTS="-XX:+UseConcMarkSweepGC \
      -XX:+UseParNewGC \
      -XX:+ExitOnOutOfMemoryError"

COPY target/nonametv-xmltvingest.jar /usr/local/jetty/nonametv-xmltvingest.jar
COPY log4j.properties /usr/local/jetty/log4j.properties

WORKDIR /usr/local/jetty

CMD java \
    -Djetty.home="$JETTY_HOME" \
    -Dsun.net.inetaddr.ttl="$SUN_NET_INETADDR_TTL" \
    -Datlas.apiKey="$ATLAS_APIKEY" \
    -Datlas.host="$ATLAS_HOST" \
    -Daws.accessKey="$AWS_ACCESSKEY" \
    -Daws.secretKey="$AWS_SECRETKEY" \
    -Dserver.port="$SERVER_PORT" \
    -Djsse.enableSNIExtension="$JSSE_ENABLESNIEXTENSION" \
    -Dlog4j.configuration="$LOG4J_CONFIGURATION" \
    -Dnimrod.log.level="$NIMROD_LOG_LEVEL" \
    -Droot.log.level="$ROOT_LOG_LEVEL" \
    -Xmx$JVM_MEMORY \
    -Xms$JVM_MEMORY \
    $JVM_OPTS \
    -jar nonametv-xmltvingest.jar
