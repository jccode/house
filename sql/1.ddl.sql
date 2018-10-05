create table seed
(
  id          int auto_increment primary key,
  url         varchar(512) not null comment 'url',
  last_fetch_time datetime null comment '上次抓取时间',
  create_time datetime default CURRENT_TIMESTAMP not null,
  update_time datetime default CURRENT_TIMESTAMP not null,
  constraint seed_url_uindex unique (url)
) comment '种子url' charset = utf8;
