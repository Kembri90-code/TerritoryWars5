CREATE TABLE achievements (
  id          TEXT PRIMARY KEY,
  key         TEXT UNIQUE NOT NULL,
  name        TEXT NOT NULL,
  description TEXT NOT NULL,
  category    TEXT NOT NULL,
  points      INTEGER NOT NULL,
  icon        TEXT NOT NULL DEFAULT '🏆'
);

CREATE TABLE user_achievements (
  user_id        TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  achievement_id TEXT NOT NULL REFERENCES achievements(id),
  unlocked_at    TIMESTAMPTZ DEFAULT NOW(),
  PRIMARY KEY (user_id, achievement_id)
);

CREATE INDEX idx_user_achievements_user ON user_achievements(user_id);

INSERT INTO achievements (id, key, name, description, category, points, icon) VALUES
('ach_first_capture',  'first_capture',  'Первый шаг',        'Захвати свою первую территорию',                    'CAPTURE',  10,  '👣'),
('ach_captures_10',    'captures_10',    'Землевладелец',     'Захвати 10 территорий',                             'CAPTURE',  20,  '🏠'),
('ach_captures_50',    'captures_50',    'Барон',             'Захвати 50 территорий',                             'CAPTURE',  50,  '🏰'),
('ach_captures_200',   'captures_200',   'Лорд',              'Захвати 200 территорий',                            'CAPTURE',  100, '👑'),
('ach_area_10k',       'area_10k',       'Первый гектар',     'Достигни суммарной площади 10 000 м²',              'CAPTURE',  30,  '🌱'),
('ach_area_1km',       'area_1km',       'Квартал',           'Достигни суммарной площади 1 км²',                  'CAPTURE',  75,  '🏙'),
('ach_area_10km',      'area_10km',      'Район',             'Достигни суммарной площади 10 км²',                 'CAPTURE',  200, '🌆'),
('ach_first_takeover', 'first_takeover', 'Захватчик',         'Отвоюй первую чужую территорию',                    'CONQUEST', 15,  '⚔'),
('ach_absorb',         'absorb',         'Поглотитель',       'Поглоти территорию противника, обойдя её снаружи',  'CONQUEST', 40,  '🌊'),
('ach_takeover_50',    'takeover_50',    'Завоеватель',       'Отвоюй 50 территорий',                              'CONQUEST', 80,  '🗡'),
('ach_walk_1km',       'walk_1km',       'Первая прогулка',   'Пройди 1 км во время захватов',                     'WALKING',  10,  '🚶'),
('ach_walk_42km',      'walk_42km',      'Марафонец',         'Пройди 42 км суммарно',                             'WALKING',  50,  '🏃'),
('ach_walk_100km',     'walk_100km',     'Путешественник',    'Пройди 100 км суммарно',                            'WALKING',  150, '🌍'),
('ach_walk_500km',     'walk_500km',     'Исследователь',     'Пройди 500 км суммарно',                            'WALKING',  500, '🗺'),
('ach_join_clan',      'join_clan',      'Командный игрок',   'Вступи в клан',                                     'CLAN',     10,  '🤝'),
('ach_fast_capture',   'fast_capture',   'Быстрый старт',     'Захвати территорию за 5 минут',                     'SPECIAL',  25,  '⚡'),
('ach_large_capture',  'large_capture',  'Геометр',           'Захвати территорию площадью более 5 000 м² за раз', 'SPECIAL',  35,  '📐'),
('ach_figure8',        'figure8',        'Восьмёрка',         'Захвати 2 и более областей одним маршрутом',        'SPECIAL',  50,  '∞'),
('ach_early_bird',     'early_bird',     'Ранняя пташка',     'Захвати территорию до 7:00 утра',                   'SPECIAL',  30,  '🌅'),
('ach_night_hunter',   'night_hunter',   'Ночной охотник',    'Захвати территорию после 23:00',                    'SPECIAL',  30,  '🌙');
