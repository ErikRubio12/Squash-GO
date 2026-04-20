-- Squash & Go: Metro Vancouver Court Seed Data
-- Run after 001_initial.sql using Supabase SQL Editor (service role)

INSERT INTO courts (name, address, latitude, longitude, number_of_courts, is_verified) VALUES
    ('Burnaby Lake Sports Complex', '3676 Kensington Ave, Burnaby, BC', 49.2488, -122.9542, 4, true),
    ('Vancouver Racquets Club', '4867 Ontario St, Vancouver, BC', 49.2281, -123.1044, 6, true),
    ('Hollyburn Country Club', '950 Cross Creek Rd, West Vancouver, BC', 49.3512, -123.2114, 5, true),
    ('Richmond Racquets Club', '8580 Bennett Rd, Richmond, BC', 49.1740, -123.1220, 4, true),
    ('Jericho Tennis Club', '3837 Point Grey Rd, Vancouver, BC', 49.2722, -123.1959, 3, true),
    ('UBC Recreation Centre', '6000 Student Union Blvd, Vancouver, BC', 49.2677, -123.2488, 3, true),
    ('North Shore Winter Club', '1325 E Keith Rd, North Vancouver, BC', 49.3105, -123.0645, 4, true),
    ('Steve Nash Fitness World - Burnaby', '4377 Beresford St, Burnaby, BC', 49.2255, -122.9994, 2, true),
    ('Arbutus Club', '2001 Nanton Ave, Vancouver, BC', 49.2429, -123.1536, 4, true),
    ('YWCA Health + Fitness Centre', '535 Hornby St, Vancouver, BC', 49.2830, -123.1194, 2, true);
