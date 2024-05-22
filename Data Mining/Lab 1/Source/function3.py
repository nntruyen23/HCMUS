import csv
import argparse

def is_numeric_float(value):
    try:
        float(value)
        return True
    except (ValueError, TypeError):
        return False

def fill_missing_value(data_file, method, columns, output_file):
    with open(data_file) as csv_file:
        csv_reader = csv.reader(csv_file, delimiter=',')
        header = next(csv_reader)

        column_indices = []
        column_types = []
        data = []

        for i, column in enumerate(header):
            if column in columns:
                column_indices.append(i)
                data.append([])

        for row in csv_reader:
            for i, value in enumerate(row):
                if i in column_indices:
                    data[column_indices.index(i)].append(value)

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
            if not b:
                column_types.append('categorical')

    if method == 'mean':
        for i, column_type in enumerate(column_types):
            if column_type == 'numeric':
                values = [float(x) if x != '' else 0 for x in data[i]]
                mean = sum(values) / len(values)
                for j in range(len(data[i])):
                    if data[i][j] == '':
                        data[i][j] = str(mean)
    elif method == 'median':
        for i, column_type in enumerate(column_types):
            if column_type == 'numeric':
                values = [float(x) if x != '' else 0 for x in data[i]]
                median = sorted(values)[len(values) // 2]
                for j in range(len(data[i])):
                    if data[i][j] == '':
                        data[i][j] = str(median)
        
    for i, column_type in enumerate(column_types):
            if column_type == 'categorical':
                counts = {}
                for value in data[i]:
                    if value != '':
                        counts[value] = counts.get(value, 0) + 1
                mode = None
                max_count = 0
                for key, value in counts.items():
                    if value > max_count:
                        max_count = value
                        mode = key
                for j in range(len(data[i])):
                    if data[i][j] == '':
                        data[i][j] = mode

    with open(output_file, 'w', newline='') as csvfile:
        csv_writer = csv.writer(csvfile)

        csv_writer.writerow(header)

        for i in range(len(data[0])):
            row = [data[j][i] for j in range(len(data))]
            csv_writer.writerow(row)

# Create an argument parser
parser = argparse.ArgumentParser(description='Impute missing values in a CSV file.')

# Add an argument for the data file
parser.add_argument('data_file', type=str, help='Name of the data CSV file')

# Add an argument for the imputation method
parser.add_argument('--method', type=str, choices=['mean', 'median'], default='mean', help='Imputation method (mean, median, mode)')

# Add an argument for the columns to impute
parser.add_argument('--columns', type=str, nargs='+', help='Columns to impute')

# Add an argument for the output file
parser.add_argument('--out', type=str, default='result.csv', help='Name of the output CSV file')

# Parse the command-line arguments
args = parser.parse_args()

# Call the fill_missing_value function with the provided arguments
fill_missing_value(args.data_file, args.method, args.columns, args.out)