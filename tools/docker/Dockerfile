ARG MP_VERSION=devel
ARG MP_DIR=/opt/midpoint
ARG MP_DIST_FILE=midpoint-dist.tar.gz
ARG JAVA_VERSION=21

ARG base_image=alpine
ARG base_image_tag=3.19

FROM ${base_image}:${base_image_tag} AS builder

ARG base_image
ARG MP_DIR
ARG MP_DIST_FILE

RUN case "${base_image}" in \
      ubuntu) \
        apt-get update -y && \
        apt-get install -y --no-install-recommends libxml2-utils && \
        apt-get clean && \
        rm -rf /var/lib/apt/lists/* ;; \
      alpine) \
        apk add --no-cache libxml2-utils bash ;; \
      rockylinux) \
        dnf install -y --nodocs libxml2 && \
        dnf clean all ;; \
    esac

COPY "${MP_DIST_FILE}" "${MP_DIR}/"

RUN tar -xzC "${MP_DIR}" -f "${MP_DIR}/${MP_DIST_FILE}" --strip-components=1 && \
    rm -f "${MP_DIR}/${MP_DIST_FILE}"

FROM ${base_image}:${base_image_tag} AS runtime

ARG MP_DIR
ARG MP_VERSION
ARG base_image
ARG base_image_tag
ARG JAVA_VERSION

LABEL org.opencontainers.image.authors="info@evolveum.com" \
      org.opencontainers.image.version="${MP_VERSION}" \
      org.opencontainers.image.title="evolveum/midpoint" \
      org.opencontainers.image.description="Evolveum midPoint Identity Management System" \
      org.opencontainers.image.url="https://evolveum.com/midpoint/" \
      org.opencontainers.image.source="https://github.com/Evolveum/midpoint.git"

ENV MP_SET_file_encoding=UTF8 \
    MP_SET_midpoint_logging_alt_enabled=true \
    MP_MEM_MAX=2048m \
    MP_MEM_INIT=1024m \
    TZ=UTC \
    MP_DIR=${MP_DIR}

COPY container_files/usr-local-bin/* /usr/local/bin/

RUN case "${base_image}" in \
      ubuntu) \
        sed 's/main$/main universe/' -i /etc/apt/sources.list && \
        apt-get update -y && \
        apt-get install -y --no-install-recommends \
          openjdk-${JAVA_VERSION}-jre-headless \
          tzdata \
          curl \
          language-pack-en \
          ca-certificates && \
        apt-get clean && \
        rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* && \
        export LC_ALL=en_US.UTF-8 LANG=en_US.UTF-8 LANGUAGE=en_US.UTF-8 && \
        groupadd -r -g 1000 midpoint && \
        useradd -r -u 1000 -g midpoint midpoint ;; \
      rockylinux) \
        dnf install -y --nodocs --setopt=install_weak_deps=False \
          java-${JAVA_VERSION}-openjdk-headless \
          tzdata \
          bash \
          which \
          glibc-langpack-en \
          ca-certificates && \
        dnf clean all && \
        export LC_ALL=en_US.UTF-8 LANG=en_US.UTF-8 LANGUAGE=en_US.UTF-8 && \
        groupadd -r -g 1000 midpoint && \
        useradd -r -u 1000 -g midpoint midpoint ;; \
      alpine) \
        apk add --no-cache \
          openjdk${JAVA_VERSION}-jre-headless \
          curl \
          libxml2-utils \
          tzdata \
          bash \
          musl \
          musl-utils \
          musl-locales \
          ca-certificates && \
        export LC_ALL=en_US.UTF-8 LANG=en_US.UTF-8 LANGUAGE=en_US.UTF-8 && \
        addgroup -S -g 1000 midpoint && \
        adduser -S -u 1000 -G midpoint midpoint ;; \
    esac && \
    mkdir -p "${MP_DIR}/bin/"

RUN case "${base_image}" in \
      ubuntu|rockylinux) \
        echo 'export LC_ALL=en_US.UTF-8' >> /etc/environment && \
        echo 'export LANG=en_US.UTF-8' >> /etc/environment && \
        echo 'export LANGUAGE=en_US.UTF-8' >> /etc/environment ;; \
      alpine) \
        echo 'export LC_ALL=en_US.UTF-8' >> /etc/profile && \
        echo 'export LANG=en_US.UTF-8' >> /etc/profile && \
        echo 'export LANGUAGE=en_US.UTF-8' >> /etc/profile ;; \
    esac

ENV LC_ALL=en_US.UTF-8 \
    LANG=en_US.UTF-8 \
    LANGUAGE=en_US.UTF-8

COPY --from=builder "${MP_DIR}" "${MP_DIR}/"

RUN chown -R midpoint:midpoint "${MP_DIR}"

RUN if [ $(grep -c "container" "${MP_DIR}/bin/midpoint.sh") -eq 0 ]; then \
      cp /usr/local/bin/midpoint.sh "${MP_DIR}/bin/midpoint.sh" && \
      echo "midpoint.sh file replaced with container version" ; \
    fi

VOLUME "${MP_DIR}/var"

HEALTHCHECK --interval=1m --timeout=30s --start-period=2m \
    CMD /usr/local/bin/healthcheck.sh

EXPOSE 8080

USER midpoint

CMD [ "/opt/midpoint/bin/midpoint.sh", "container" ]