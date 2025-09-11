ARG MP_VERSION=devel
ARG MP_DIR=/opt/midpoint
ARG MP_DIST_FILE=midpoint-dist.tar.gz
ARG JAVA_VERSION=21

ARG base_image=alpine
ARG base_image_tag=latest

FROM ${base_image}:${base_image_tag}

ARG base_image
ARG MP_VERSION
ARG MP_DIR
ARG MP_DIST_FILE

RUN if [ "${base_image}" = "ubuntu" ]; \
  then apt-get update -y && apt-get install -y libxml2-utils ; \
  else if [ "${base_image}" = "alpine" ]; \
  then apk --update add --no-cache libxml2-utils bash ; \
  fi ; fi

COPY ${MP_DIST_FILE} ${MP_DIR}/

RUN tar -xzC ${MP_DIR} -f ${MP_DIR}/${MP_DIST_FILE} --strip-components=1 && \
    rm -f ${MP_DIR}/${MP_DIST_FILE}


FROM ${base_image}:${base_image_tag}

ARG MP_DIR
ARG MP_VERSION
ARG base_image
ARG base_image_tag
ARG JAVA_VERSION

LABEL org.opencontainers.image.authors="info@evolveum.com"
LABEL org.opencontainers.image.version="${MP_VERSION}"
LABEL org.opencontainers.image.title="evolveum/midpoint"

ENV MP_SET_file_encoding=UTF8 \
 MP_SET_midpoint_logging_alt_enabled=true \
 MP_MEM_MAX=2048m \
 MP_MEM_INIT=1024m \
 TZ=UTC \
 LC_ALL=en_US.UTF-8 \
 LANG=en_US.UTF-8 \
 LANGUAGE=en_US.UTF-8 \
 MP_DIR=${MP_DIR}

COPY container_files/usr-local-bin/* /usr/local/bin/

RUN if [ "${base_image}" = "ubuntu" ]; \
  then sed 's/main$/main universe/' -i /etc/apt/sources.list && \
       apt-get update -y && \
       apt-get install -y openjdk-${JAVA_VERSION}-jre-headless tzdata curl language-pack-en && \
       apt-get clean && \
       rm -rf /var/lib/apt/lists/*/tmp/* /var/tmp/* && \
       groupadd -r midpoint && \
       useradd -r -g midpoint midpoint ; \
  else if [ "${base_image}" = "rockylinux" ]; \
  then dnf update -y && dnf install -y java-${JAVA_VERSION}-openjdk-headless tzdata bash which glibc-langpack-en && dnf clean all -y && \
       groupadd -r midpoint && \
       useradd -r -g midpoint midpoint ; \
  else apk --update add --no-cache openjdk${JAVA_VERSION}-jre-headless curl libxml2-utils tzdata bash fontconfig ttf-dejavu musl musl-utils musl-locales && \
       addgroup -S midpoint && \
       adduser -S -G midpoint midpoint ; \
  fi ; fi ; \
  mkdir -p "${MP_DIR}/bin/"

COPY --from=0 ${MP_DIR} ${MP_DIR}/

RUN chown -R midpoint:midpoint ${MP_DIR}

RUN if [ $(grep -c "container" ${MP_DIR}/bin/midpoint.sh) -eq 0 ]; then \
  cp /usr/local/bin/midpoint.sh ${MP_DIR}/bin/midpoint.sh && echo "midpoint.sh file replaced" ; fi

VOLUME ${MP_DIR}/var

HEALTHCHECK --interval=1m --timeout=30s --start-period=2m CMD /usr/local/bin/healthcheck.sh

EXPOSE 8080

USER midpoint

CMD [ "/opt/midpoint/bin/midpoint.sh", "container" ]