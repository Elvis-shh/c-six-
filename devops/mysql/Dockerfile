ARG MYSQL_IMAGE=mysql:8.0
FROM ${MYSQL_IMAGE}
COPY conf.d/ /etc/mysql/conf.d/
RUN chmod 0644 /etc/mysql/conf.d/*.cnf
