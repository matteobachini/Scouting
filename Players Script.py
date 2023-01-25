import pandas as pd
import os
import glob
import math 

def isNaN(string):
    return string != string

#path = '/Users/andreasottile/Downloads/roba/csv/';
path = 'C:/Users/HomeWorking/OneDrive - University of Pisa/Esami/Large Scale And Multistructured Databases/Progetto Large Scale/Dataset/Dataset completo';
csv_files = glob.glob(os.path.join(path, "*.csv"))

path22 = 'C:/Users/HomeWorking/OneDrive - University of Pisa/Esami/Large Scale And Multistructured Databases/Progetto Large Scale/Dataset/Dataset completo/players_22.csv';
df22 = pd.read_csv(path22, index_col=False);

df22.columns.values[0] = 'FifaID';
df22.set_index = 'FifaID';

df_array = []; 

for f in csv_files:
    if f==path22:
        continue;
    df = pd.read_csv(f);
    df.columns.values[0] = 'FifaID';
    df['Dataset'] = f;
    df_array.append(df);

df_concat = pd.concat(df_array, ignore_index=True)

printed_columns = ['FifaID', 
# 'long_name_df_new', 'long_name_df22', 'Name', 
# 'overall_df_new', 'overall_df22', 'Overall',
#  'player_positions_df_new', 'player_positions_df22', 'Best Position', 
#  'age_df_new', 'age_df22', 'Age', 
#  'preferred_foot_df_new', 'preferred_foot_df22', 'Preferred Foot', 
# 'club_name_df_new', 'club_name_df22', 'Club', 
'fullName', 'rate', 'role', 'age', 'foot', 'team', 'photo', 'codPlayer'
, 'Dataset'
];

df_new = df_concat.drop_duplicates(subset=['FifaID'], keep='last');

#df_new.to_csv("file1.csv", index=False, encoding='utf-16');

df_new.set_index='FifaID';
df22.set_index='FifaID';
df_new = df_new.merge(df22, on='FifaID', how='right', suffixes=['_df_new', '_df22']);

df_new['fullName'] = '';
df_new['rate'] = '';
df_new['role'] = '';
df_new['age'] = '';
df_new['foot'] = '';
df_new['team'] = '';
df_new['photo'] = '';
df_new['codPlayer'] = '';

for i, row in df_new.iterrows():
    df_new.loc[i, 'codPlayer'] = i+1;
    ##fullName
    if not isNaN(row['Name']):
        result = ''.join([i for i in row['Name'] if not i.isdigit()])
        df_new.loc[i, 'fullName'] = result;
    else:
        if not isNaN(row['long_name_df22']):
            df_new.loc[i, 'fullName'] = row['long_name_df22'];
        else:
            df_new.loc[i, 'fullName'] = row['long_name_df_new'];

    ##rate
    if not isNaN(row['Overall']):
        df_new.loc[i, 'rate'] = math.trunc(row['Overall']);
    else:
        if not isNaN(row['overall_df22']):
            df_new.loc[i, 'rate'] = math.trunc(row['overall_df22']);
        else:
            df_new.loc[i, 'rate'] = math.trunc(row['overall_df_new']);
    
    ##role
    if not isNaN(row['Best Position']):
        df_new.loc[i, 'role'] = row['Best Position'];
    else:
        if not isNaN(row['player_positions_df22']):
            df_new.loc[i, 'role'] = row['player_positions_df22'].split(',', 1)[0];
        else:
            df_new.loc[i, 'role'] = row['player_positions_df_new'].split(',', 1)[0];

    ##age
    if not isNaN(row['Age']):
        df_new.loc[i, 'age'] = math.trunc(row['Age']);
    else:
        if not isNaN(row['age_df22']):
            df_new.loc[i, 'age'] = math.trunc(row['age_df22']);
        else:
            df_new.loc[i, 'age'] = math.trunc(row['age_df_new']);

    ##foot
    if not isNaN(row['Preferred Foot']):
        df_new.loc[i, 'foot'] = row['Preferred Foot'];
    else:
        if not isNaN(row['age_df22']):
            df_new.loc[i, 'foot'] = row['preferred_foot_df22'];
        else:
            df_new.loc[i, 'foot'] = row['preferred_foot_df_new'];

    ##team
    if not isNaN(row['Club']):
        df_new.loc[i, 'team'] = row['Club'];
    else:
        if not isNaN(row['club_name_df22']):
            df_new.loc[i, 'team'] = row['club_name_df22'];
        else:
            df_new.loc[i, 'team'] = row['club_name_df_new'];

    ##photo
    if not isNaN(row['Photo']):
        df_new.loc[i, 'photo'] = row['Photo'];
    else:
        if not isNaN(row['player_face_url_df22']):
            df_new.loc[i, 'photo'] = row['player_face_url_df22'];
        else:
            df_new.loc[i, 'photo'] = row['player_face_url_new'];

df_new.to_csv("Players.csv", index=False, encoding='utf-16', columns=printed_columns);