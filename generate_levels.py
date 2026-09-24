import json
import random
import os

DIR_VECTORS = {
    'UP': (-1, 0),
    'DOWN': (1, 0),
    'LEFT': (0, -1),
    'RIGHT': (0, 1)
}

def generate_level(level_id, size, difficulty, target_density, random_inst):
    grid_cells = size * size
    target_count = int(grid_cells * target_density)

    def can_head_exit(head_r, head_c, direction, occupied_set):
        dr, dc = DIR_VECTORS[direction]
        curr_r, curr_c = head_r + dr, head_c + dc
        while 0 <= curr_r < size and 0 <= curr_c < size:
            if (curr_r, curr_c) in occupied_set:
                return False
            curr_r += dr
            curr_c += dc
        return True

    occupied_board = set()
    arrows = []

    if difficulty == "EASY":
        allowed_lengths = [3, 4]
    elif difficulty == "MEDIUM":
        allowed_lengths = [3, 4, 5]
    else:
        allowed_lengths = [4, 5, 6, 7]

    attempts = 0
    max_attempts = 3000

    while len(occupied_board) < target_count and attempts < max_attempts:
        attempts += 1

        length = random_inst.choice(allowed_lengths)
        direction = random_inst.choice(['UP', 'DOWN', 'LEFT', 'RIGHT'])
        dr, dc = DIR_VECTORS[direction]

        head_r = random_inst.randint(0, size - 1)
        head_c = random_inst.randint(0, size - 1)

        if (head_r, head_c) in occupied_board:
            continue

        if not can_head_exit(head_r, head_c, direction, occupied_board):
            continue

        prev_r = head_r - dr
        prev_c = head_c - dc

        if not (0 <= prev_r < size and 0 <= prev_c < size):
            continue
        if (prev_r, prev_c) in occupied_board:
            continue

        current_path = [(prev_r, prev_c), (head_r, head_c)]
        visited_in_arrow = {(prev_r, prev_c), (head_r, head_c)}

        curr_back_r, curr_back_c = prev_r, prev_c
        back_dir = (-dr, -dc)
        turn_cooldown = 0

        for _ in range(length - 2):
            possible_back_dirs = [back_dir]
            if turn_cooldown == 0:
                if back_dir[0] != 0:
                    possible_back_dirs.extend([(0, 1), (0, -1)])
                else:
                    possible_back_dirs.extend([(1, 0), (-1, 0)])

            random_inst.shuffle(possible_back_dirs)

            step_added = False
            for b_dr, b_dc in possible_back_dirs:
                next_r = curr_back_r + b_dr
                next_c = curr_back_c + b_dc

                if (0 <= next_r < size and 0 <= next_c < size and
                    (next_r, next_c) not in occupied_board and
                    (next_r, next_c) not in visited_in_arrow):

                    current_path.insert(0, (next_r, next_c))
                    visited_in_arrow.add((next_r, next_c))
                    if (b_dr, b_dc) != back_dir:
                        back_dir = (b_dr, b_dc)
                        turn_cooldown = 2
                    elif turn_cooldown > 0:
                        turn_cooldown -= 1

                    curr_back_r, curr_back_c = next_r, next_c
                    step_added = True
                    break

            if not step_added:
                break

        if len(current_path) >= 2 and can_head_exit(head_r, head_c, direction, occupied_board):
            arrows.append({'points': current_path, 'direction': direction})
            for r, c in current_path:
                occupied_board.add((r, c))
            attempts = 0

    arrows.reverse()
    for i, arr in enumerate(arrows):
        arr['id'] = i + 1
        arr['points'] = [{'first': p[0], 'second': p[1]} for p in arr['points']]

    return {
        'id': level_id,
        'size': size,
        'difficulty': difficulty,
        'arrows': arrows
    }

def main():
    print("Generating 3000 clean, winding snake levels...")
    levels = []

    for i in range(1, 101):
        rnd = random.Random(i * 777 + 42)
        size = 5 if i <= 30 else (6 if i <= 70 else 7)
        levels.append(generate_level(i, size, "EASY", 0.75, rnd))

    for i in range(101, 301):
        rnd = random.Random(i * 777 + 42)
        size = 7 if i <= 200 else 8
        levels.append(generate_level(i, size, "MEDIUM", 0.82, rnd))

    for i in range(301, 3001):
        rnd = random.Random(i * 777 + 42)
        size = 9 if i <= 800 else (10 if i <= 1800 else 11)
        levels.append(generate_level(i, size, "HARD", 0.88, rnd))

    out_dir = "app/src/main/assets/levels"
    os.makedirs(out_dir, exist_ok=True)

    for start in range(1, 3001, 100):
        end = start + 99
        chunk = [lvl for lvl in levels if start <= lvl["id"] <= end]
        filename = f"{out_dir}/levels_{start}_{end}.json"
        with open(filename, 'w') as f:
            json.dump(chunk, f)
        print(f"Saved: {filename}")

if __name__ == "__main__":
    main()
