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

global_arrow_pos = [0.01, -1.5840298512464805, 0.0066421353727];

pitch_input = read_file('pitchInput', 'shared_text');
global_pitch_input = l();
for(pitch_input,
    put(global_pitch_input, _i, number(_));
);

pitch_output = read_file('pitchOutput', 'shared_text');
global_pitch_output = l();
for(pitch_output,
    put(global_pitch_output, _i, number(_));
);

arrow_output = read_file('arrowOutput', 'shared_text');
global_arrow_output = l();
for(arrow_output,
    put(global_arrow_output, _i, number(_));
);

aim_target() -> (
    min_target_dist = 1000;
    for(global_player_names, 
        if(player(_) != null,
            current_target = player(_);
            current_target_pos = query(current_target, 'pos');
            current_target_dis = current_target_pos - global_arrow_pos;
            current_target_mag = norm(current_target_dis);
            if(current_target_mag < min_target_dist, target = current_target, min_target_dist = current_target_mag)
        );
    );
    
    target_pos = query(target, 'pos') + [0, 0.3, 0];
    target_dis = target_pos - global_arrow_pos;

    target_mag = norm(target_dis);
    target_xz = norm([get(target_dis, 0), get(target_dis, 2)]);
    target_pitch = atan2(-get(target_dis, 1), target_xz);

    pitch_output = l();
    loop(70, put(pitch_output, _, get(global_pitch_output, _)));

    corrected_speed = interpolate(pitch_output, global_arrow_output, target_pitch);
    target_time = cover_distance(corrected_speed, target_mag);

    target_vel = query(target, 'motion');
    target_future = target_dis + target_vel*target_time;

    target_xz = norm([get(target_future, 0), get(target_future, 2)]);
    target_pitch = atan2(-get(target_future, 1), target_xz);
    target_yaw = atan2(-get(target_future, 0), get(target_future, 2));
    
    pitch_output = l();
    loop(70, put(pitch_output, _, get(global_pitch_output, (target_time * 70) + _)));
    corrected_pitch = interpolate(pitch_output, global_pitch_input, target_pitch);
    run(str('player %s look %f %f', 'MrMetre', corrected_pitch, target_yaw));
);

interpolate(x, y, t) -> (
    new_min = 1e7;
    for(x,
        cur_min = abs(t - _);
        if(cur_min < new_min,
            mindex = _i;
            new_min = cur_min;
        );
    );

    if(get(x, mindex) > t, mindex = mindex - 1);
    if(mindex >= length(x) - 1, mindex = mindex - 1);

    xL = get(x, mindex);
    xU = get(x, mindex + 1);
    int = (t - xL) / (xU - xL);
    yL = get(y, mindex);
    yU = get(y, mindex + 1);
    tO = int * (yU - yL) + yL;

    return(tO);
);

cover_distance(v0, d) -> (
    v = v0;
    n = 0;
    dist = 0;
    while(dist < d,
        dist = dist + v;
        v = v*0.99;
        n = n + 1;
    );
    return(n);
);

norm(vec) -> (
    return(sqrt(reduce(vec, _a + _^2, 0)))
);