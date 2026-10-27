alter table departments
    add column organization_id bigint null,
    add constraint departments_organization_id_fk
        foreign key (organization_id) references organizations (id);

alter table assessors
    add column organization_id bigint null,
    add constraint assessors_organization_id_fk
        foreign key (organization_id) references organizations (id);

alter table mr_reviews
    add column organization_id bigint null,
    add constraint mr_reviews_organization_id_fk
        foreign key (organization_id) references organizations (id);

insert into organizations (name, org_type, status, created_at, updated_at)
select seed.name, seed.org_type, seed.status, seed.created_at, seed.updated_at
from (
    select 'Ain Shams University Specialized Hospital' as name,
           'HOSPITAL' as org_type,
           'Active' as status,
           current_timestamp(6) as created_at,
           current_timestamp(6) as updated_at
    where not exists (
        select 1 from organizations
        where name = 'Ain Shams University Specialized Hospital' and deleted_at is null
    )
    and (
        exists (select 1 from departments)
        or exists (select 1 from assessors)
        or exists (select 1 from mr_reviews)
    )
) seed;

update departments d
join organizations o
    on o.name = 'Ain Shams University Specialized Hospital' and o.deleted_at is null
set d.organization_id = o.id
where d.organization_id is null;

update assessors a
join organizations o
    on o.name = 'Ain Shams University Specialized Hospital' and o.deleted_at is null
set a.organization_id = o.id
where a.organization_id is null;

update mr_reviews r
join organizations o
    on o.name = 'Ain Shams University Specialized Hospital' and o.deleted_at is null
set r.organization_id = o.id
where r.organization_id is null;

update users u
join organizations o
    on o.name = 'Ain Shams University Specialized Hospital' and o.deleted_at is null
set u.organization_id = o.id
where u.organization_id is null and u.is_super_admin = 0;

alter table departments modify column organization_id bigint not null;
alter table assessors modify column organization_id bigint not null;
alter table mr_reviews modify column organization_id bigint not null;
