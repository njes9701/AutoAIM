__config() -> {
  'scope' -> 'global'  // prevents it from attaching to players
};

// List your players
global_player_names = [
    '001',
    '002',
    '003',
    '004'
];

// Create a map with each column name mapped to a list of nulls
global_rotation_list = m();
for(global_player_names, 
        col = _;
        rotation_list = read_file(col, 'shared_text');
        pairs = l();
        for(rotation_list,
            pair = split('\\,',_);
            yaw = number(get(pair,0));
            pitch = number(get(pair,1));
            put(pairs, _i, [yaw, pitch]);
        );
    put(global_rotation_list, col, pairs);
);

rotate_players() -> (
    for(global_player_names,
        tmax = length(get(global_rotation_list, _)) - 1;
        t = scoreboard('counter', 'tCount');

        // Ensure index cannot surpass array length
        if(t > tmax, t = tmax);

        yaw = get(get(get(global_rotation_list, _), t), 0);
        pitch = get(get(get(global_rotation_list, _), t), 1);
        run(str('player %s look %f %f', _, pitch, yaw));
    );
)