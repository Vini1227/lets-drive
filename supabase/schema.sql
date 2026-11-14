-- Let's Drive - Schema Geoespacial (PostGIS + Supabase Realtime)
-- Alinhado com as sugestões do professor:
-- 1. Consultas espaciais nativas (PostGIS geography(Point, 4326) + ST_DWithin / ST_Distance)
-- 2. Limite máximo de separação configurável pelo usuário (max_separation_meters)
-- 3. Suporte a participantes em movimento (MOVING_VEHICLE / WALKING_TRANSIT) ou parados esperando (STATIONARY_HOST)
-- 4. Foco 100% em localização compartilhada e coordenação (sem álbum de fotos)

-- Habilitar extensão geoespacial PostGIS
create extension if not exists postgis;

-- 1. Tabela de Viagens / Sessões de Acompanhamento (Trips)
create table if not exists public.trips (
    id uuid primary key default gen_random_uuid(),
    invite_code text unique not null,
    name text not null,
    origin_address text not null,
    destination_name text not null,
    destination_point geography(Point, 4326) not null,
    max_separation_meters double precision not null default 2000.0, -- Limite configurável p/ alerta (ex: 2000m = 2km)
    creator_id uuid not null,
    status text not null default 'ACTIVE' check (status in ('ACTIVE', 'COMPLETED', 'CANCELLED')),
    selected_meeting_point_id uuid,
    created_at timestamptz not null default now()
);

-- 2. Tabela de Participantes (Em movimento ou aguardando no destino)
create table if not exists public.participants (
    id uuid primary key default gen_random_uuid(),
    trip_id uuid not null references public.trips(id) on delete cascade,
    user_id uuid not null,
    display_name text not null,
    vehicle_or_status_label text not null,
    role text not null default 'MOVING_VEHICLE' check (role in ('MOVING_VEHICLE', 'STATIONARY_HOST', 'WALKING_TRANSIT')),
    color_hex text not null default '#38F0FC',
    is_sharing_location boolean not null default true,
    joined_at timestamptz not null default now(),
    unique (trip_id, user_id)
);

-- 3. Tabela Geoespacial de Posições em Tempo Real
create table if not exists public.participant_locations (
    participant_id uuid primary key references public.participants(id) on delete cascade,
    trip_id uuid not null references public.trips(id) on delete cascade,
    coords geography(Point, 4326) not null,
    speed_kmh integer not null default 0,
    is_separated boolean not null default false,
    updated_at timestamptz not null default now()
);

-- Índice espacial GIST para consultas rápidas de distância e raio (equivalente ao 2dsphere do MongoDB)
create index if not exists idx_participant_locations_coords
    on public.participant_locations using gist (coords);

-- 4. Função Geoespacial: Verifica quais participantes em movimento ultrapassaram o max_separation_meters do líder/grupo
create or replace function public.check_trip_separation(p_trip_id uuid, p_leader_participant_id uuid)
returns table (
    participant_id uuid,
    display_name text,
    distance_meters double precision,
    exceeded_threshold boolean
)
language sql
security definer
as $$
    with leader as (
        select l.coords, t.max_separation_meters
        from public.participant_locations l
        join public.trips t on t.id = l.trip_id
        where l.trip_id = p_trip_id
          and l.participant_id = p_leader_participant_id
    )
    select
        p.id as participant_id,
        p.display_name,
        ST_Distance(pl.coords, leader.coords) as distance_meters,
        not ST_DWithin(pl.coords, leader.coords, leader.max_separation_meters) as exceeded_threshold
    from public.participant_locations pl
    join public.participants p on p.id = pl.participant_id
    cross join leader
    where pl.trip_id = p_trip_id
      and p.role != 'STATIONARY_HOST'
      and p.id != p_leader_participant_id;
$$;

-- 5. Habilitar Supabase Realtime para propagação de posições e participantes ao vivo
alter publication supabase_realtime add table public.trips;
alter publication supabase_realtime add table public.participants;
alter publication supabase_realtime add table public.participant_locations;

-- 6. Políticas de Acesso RLS (Row Level Security) permissivas para desenvolvimento e uso seguro
alter table public.trips enable row level security;
alter table public.participants enable row level security;
alter table public.participant_locations enable row level security;

create policy "Permitir leitura pública ou autenticada de viagens"
    on public.trips for select using (true);
create policy "Permitir criação de viagens"
    on public.trips for insert with check (true);
create policy "Permitir atualização de viagens"
    on public.trips for update using (true);

create policy "Permitir leitura pública ou autenticada de participantes"
    on public.participants for select using (true);
create policy "Permitir inserção de participantes"
    on public.participants for insert with check (true);
create policy "Permitir atualização de participantes"
    on public.participants for update using (true);

create policy "Permitir leitura pública ou autenticada de localizações"
    on public.participant_locations for select using (true);
create policy "Permitir inserção e atualização de localizações"
    on public.participant_locations for all using (true) with check (true);
