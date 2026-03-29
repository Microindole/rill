insert into sql_snippet (title, description, sql_text, created_at, updated_at)
select '查看用户列表示例', '默认演示用 snippet', 'select * from users;', current_timestamp, current_timestamp
where not exists (select 1 from sql_snippet where title = '查看用户列表示例');

insert into demo_scenario (title, description, sql_script, created_at, updated_at)
select '初始化演示用户表', '默认演示场景，创建表并写入一条用户数据',
       'create database demo; use demo; create table users (id int primary key, name varchar(32)); insert into users (id, name) values (1, ''alice'');',
       current_timestamp, current_timestamp
where not exists (select 1 from demo_scenario where title = '初始化演示用户表');
