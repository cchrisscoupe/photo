
-- Variants

-- Bidirectional map of related photos
create table photo_variations (
	original_id integer not null,
	variant_id integer not null,
	foreign key(original_id) references album(id) on delete cascade,
	foreign key(variant_id) references album(id) on delete cascade
)
;

grant all on photo_variations to nobody
;
create unique index photo_variations_uniq
	on photo_variations(original_id, variant_id)
;
