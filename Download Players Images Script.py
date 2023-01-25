import requests
import pandas as pd


df = pd.read_csv('C:/Users/andre/OneDrive - University of Pisa/Script Calciatori/dataset.csv',encoding="utf-16", usecols = ['Foto'])
for numero in range(len(df)):
 #   print(df.loc[numero, 'Foto']);
    url= df.loc[numero, 'Foto']
    r = requests.get(url, allow_redirects=True)
    open('C:/Users/andre/OneDrive - University of Pisa/Script Calciatori/immagini/immagine' + str(numero) + ".png", 'wb').write(r.content)
#url = 'https://cdn.sofifa.net/players/158/023/23_60.png'
#r = requests.get(url, allow_redirects=True)

#open('immagine.png', 'wb').write(r.content)
