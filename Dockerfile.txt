FROM artifactory.global.standardchartered.com/gv-images-products/scb-bases/gts-2730/jre:17.0.10.0.7-micro-472071
ARG JAR_FILE
COPY ${JAR_FILE} /opt/app/service.jar
