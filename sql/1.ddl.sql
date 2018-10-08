create table seed
(
  id          int auto_increment primary key,
  url         varchar(512) not null comment 'url',
  last_fetch_time datetime null comment '上次抓取时间',
  create_time datetime default CURRENT_TIMESTAMP not null,
  update_time datetime default CURRENT_TIMESTAMP not null,
  constraint seed_url_uindex unique (url)
) comment '种子url' charset = utf8;

create table house
(
  id int auto_increment primary key,
  url varchar(128) not null comment 'url',
  title varchar(128) comment '标题',
  housing_estate varchar(128) comment '小区',
  house_type varchar(128) comment '户型',
  area float comment '面积',
  total_price double comment '总价',
  unit_price double comment '单价',
  orientation varchar(128) comment '朝向',
  decoration varchar(128) comment '装修(简装,精装,其他)',
  elevator varchar(128) comment '电梯(有/无)',
  floor_desc varchar(128) comment '楼层',
  age int(8) comment '年份',
  sub_district varchar(128) comment '片区',
  publish_date_desc varchar(128) comment '发布时间',
  tags varchar(128) comment '标签',
  last_fetch_time datetime null comment '上次抓取时间',
  create_time datetime default CURRENT_TIMESTAMP not null,
  update_time datetime default CURRENT_TIMESTAMP not null,
  constraint house_url_uindex unique (url)
) comment '房屋' charset = utf8;

create table housing_estate
(
  id int auto_increment primary key,
  no varchar(32) not null comment '编号',
  name varchar(64) not null comment '名称',
  avg_price double comment '均价',
  lowest_price double comment '最低价',
  deal_hist varchar(2048) null comment '成交记录',
  last_fetch_time datetime null comment '上次抓取时间',
  create_time datetime default CURRENT_TIMESTAMP not null,
  update_time datetime default CURRENT_TIMESTAMP not null,
  constraint housing_estate_no_uindex unique (no)
) comment '小区' charset = utf8;
