drop table if exists Book;
create table Book (
	id serial primary key,
	title varchar(255),
	pages integer not null
);
