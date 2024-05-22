import csv
import argparse

def delete_col_with_threshold(data_file, output_file, threshold=0.1):
    with open(data_file) as csv_file:
        csv_reader = csv.reader(csv_file, delimiter=',')
        header = next(csv_reader)
        line_count = 0
        for row in csv_reader:
            line_count += 1
        max_missing_values = line_count * threshold
        csv_file.seek(0)

        count_missing_column = [0] * len(header)
        for row in csv_reader:
            for idx, value in enumerate(row):
                if value.strip() == '':
                    count_missing_column[idx] += 1

        csv_file.seek(0)
        with open(output_file, 'w', newline='') as csvfile:
            csv_writer = csv.writer(csvfile)
            for row in csv_reader:
                filtered_row = [value for i, value in enumerate(row) if count_missing_column[i] <= max_missing_values]
                csv_writer.writerow(filtered_row)

# Create an argument parser
parser = argparse.ArgumentParser(description='Delete columns with a high percentage of missing values from a CSV file.')

# Add an argument for the data file
parser.add_argument('data_file', type=str, help='Name of the data CSV file')

# Add an argument for the threshold
parser.add_argument('--threshold', type=float, default=0.1, help='Threshold for the maximum allowed percentage of missing values per column')

# Add an argument for the output file
parser.add_argument('--output_file', type=str, default='output_dataset_5.csv', help='Name of the output CSV file')

# Parse the command-line arguments
args = parser.parse_args()

# Call the delete_col_with_threshold function with the provided arguments
delete_col_with_threshold(args.data_file, args.output_file, args.threshold)