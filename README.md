# Igor_Podgorniak_Java_Wroclaw
Project for Ocado Internship


##Sposób działania algorytmu
Na początku algorytm tworzy wszystkie możliwe plany płatności dla wszystkich zamówień, opierając się o 5 możliwości:
- całkowita płatność kartą z rabatem
- całkowita płatność punktami z rabatem
- częściowa płatność punktami z rabatem
- brak rabatu z częściową płatnością punktami
- brak rabatu

Nastepnie wybiera najlepsze (przynoszące największy rabat) plany dla płatności kartą (mZysk i BosBankrut) i je opłaca, później dla płatności punktami i je również opłaca. Ostatecznie sprawdza te, które nie zostały opłacone i wybiera dla nich największy możliwy rabat.

Prawdopodobnie to podejście (opisane w poprzednich 2 zdaniach) nie pozwoliło mi na uzyskanie prawidłowych wyników, niestety z przyczyny braku czasu, nie udało mi się już go zmienić i pokryć testami.
