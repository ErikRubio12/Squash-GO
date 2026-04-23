-- Seed: Metro Vancouver squash venues (MVP pilot list).
-- Coordinates are approximate — verify against Google Maps before launch.
-- Idempotent: skips inserts where a court with the same name already exists.

INSERT INTO courts (name, address, latitude, longitude, number_of_courts, phone_number, website, is_verified)
SELECT * FROM (VALUES
    (
        'Jericho Tennis Club',
        '3837 Point Grey Rd, Vancouver, BC V6R 1B3',
        49.2726, -123.1864,
        4, '+1 604-224-3536', 'https://jerichotennisclub.ca', TRUE
    ),
    (
        'Vancouver Lawn Tennis & Badminton Club',
        '1630 W 15th Ave, Vancouver, BC V6J 2K7',
        49.2556, -123.1467,
        6, '+1 604-732-9141', 'https://vltbc.com', TRUE
    ),
    (
        'Hollyburn Country Club',
        '950 Cross Creek Rd, West Vancouver, BC V7S 2X9',
        49.3604, -123.1506,
        5, '+1 604-922-0161', 'https://hollyburn.org', TRUE
    ),
    (
        'Arbutus Club',
        '2001 Nanton Ave, Vancouver, BC V6J 4W3',
        49.2470, -123.1597,
        5, '+1 604-736-2911', 'https://arbutusclub.com', TRUE
    ),
    (
        'North Shore Winter Club',
        '1325 E Keith Rd, North Vancouver, BC V7J 1J3',
        49.3147, -123.0544,
        4, '+1 604-985-4135', 'https://nswc.ca', TRUE
    ),
    (
        'UBC Birdcoop Fitness Centre',
        '6000 Student Union Blvd, Vancouver, BC V6T 1Z1',
        49.2666, -123.2503,
        6, NULL, 'https://recreation.ubc.ca', TRUE
    )
) AS seed(name, address, latitude, longitude, number_of_courts, phone_number, website, is_verified)
WHERE NOT EXISTS (
    SELECT 1 FROM courts WHERE courts.name = seed.name
);
