drop table Book if exists;
create table Book (
	id bigint not null auto_increment,
	title varchar(255),
	pages integer not null,
	primary key (id)
);
