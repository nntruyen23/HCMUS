import csv
import argparse
import statistics

def is_numeric_float(value):
    try:
        float(value)
        return True
    except ValueError:
        return False

def min_max_normalize(column):
    min_val = min(column)
    max_val = max(column)
    if min_val == max_val:
        normalized_column = [0.0 for _ in column]
    else:
        normalized_column = [(x - min_val) / (max_val - min_val) for x in column]
    return normalized_column

def z_score_normalize(column, mean, std_dev):
    normalized = [(x - mean) / std_dev for x in column]
    return normalized

def calculate_mean_and_std(column):
    mean = statistics.mean(column)
    std_dev = statistics.stdev(column)
    return mean, std_dev

def normalize(data_file, output_file, method):
    with open(data_file) as csv_file:
        csv_reader = csv.reader(csv_file, delimiter=',')
        header = next(csv_reader)
        column_types = []
        data = []
        for _ in range(len(header)):
            data.append([])

        for row in csv_reader:
            for i, value in enumerate(row):
                data[i].append(value)
        for i in range(len(data)):
            b = False
            for j in data[i]:
                if j.isnumeric() or is_numeric_float(j):
                    column_types.append('numeric')
                    b = True
                    break
                else:
                    if j.strip() == '':
                        continue
            if b == False:
                column_types.append('categorical')
        
        for i, column_type in enumerate(column_types):
            if column_type == 'numeric':
                column_to_normalize = [float(x) if x != '' else 0 for x in data[i]]
                if method == 'min-max':
                    min_max_normalized_column = min_max_normalize(column_to_normalize)
                    for j in range(len(data[i])):
                        data[i][j] = str(min_max_normalized_column[j])
                elif method == 'z-score':
                    mean, std_dev = calculate_mean_and_std(column_to_normalize)
                    z_score_normalized_column = z_score_normalize(column_to_normalize, mean, std_dev)
                    for j in range(len(data[i])):
                        data[i][j] = str(z_score_normalized_column[j])

    with open(output_file, 'w', newline='') as csvfile:
        csv_writer = csv.writer(csvfile)
        csv_writer.writerow(header)
        for i in range(len(data[0])):
            row = [data[j][i] for j in range(len(header))]
            csv_writer.writerow(row)

# Create an argument parser
parser = argparse.ArgumentParser(description='Normalize numeric columns in a CSV file.')

# Add an argument for the data file
parser.add_argument('data_file', type=str, help='Name of the data CSV file')

# Add an argument for the normalization method
parser.add_argument('--method', type=str, default='min-max', choices=['min-max', 'z-score'], help='Normalization method for numeric columns: "min-max" for min-max normalization, "z-score" for z-score normalization')

# Add an argument for the output file
parser.add_argument('--output_file', type=str, default='output_dataset_7.csv', help='Name of the output CSV file')

# Parse the command-line arguments
args = parser.parse_args()

# Call the normalize function with the provided arguments
normalize(args.data_file, args.output_file, args.method)