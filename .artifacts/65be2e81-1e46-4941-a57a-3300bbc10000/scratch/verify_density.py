import json
import os

def check_density(file_path):
    with open(file_path, 'r') as f:
        levels = json.load(f)

    densities = []
    for level in levels:
        size = level['size']
        total_cells = size * size
        occupied_cells = sum(len(arrow['points']) for arrow in level['arrows'])
        density = occupied_cells / total_cells
        densities.append(density)

    avg_density = sum(densities) / len(densities)
    min_density = min(densities)
    max_density = max(densities)

    return avg_density, min_density, max_density

base_path = 'app/src/main/assets/levels/'
files = sorted([f for f in os.listdir(base_path) if f.endswith('.json')])

print(f"{'File':<25} | {'Avg Dens':<8} | {'Min Dens':<8} | {'Max Dens':<8}")
print("-" * 55)
for file in files:
    avg, dmin, dmax = check_density(os.path.join(base_path, file))
    print(f"{file:<25} | {avg:0.3f}    | {dmin:0.3f}    | {dmax:0.3f}")
