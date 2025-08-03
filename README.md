AutoAIM 模組程式靈感來自cubicmetre 的 arrow_auto_aim  Scarpet

這是自動瞄準模組專門給 cubicmetre 的Air Defence System

自動根據伺服器與客戶端延遲來自動補償射擊角度

/autoaim compensation [-10~10] 預設2  瞄準位置提前幾個gametick

/autoaim compensation auto 自動偵測網路延遲補償射擊角度，初始數值會依照目前 compensation 數值來增加或減少

/autoaim ping 目前客戶端與伺服器之間的延遲

/autoaim position x y z 根據你防空系統箭矢發射位置來設定 預設 0.01, -1.5840298512464805, 0.0066421353727  
如果沒設定則無法運作。

如何獲取弓箭位置? 請打開機器並且使用 /data get entity @n[type=minecraft:arrow]
請使用/tick step 來觀察箭矢位置
取得發射前1 gt 弓箭位置 Pos: [0.010000000000000009d, -1.5840298512464805d, 0.0066421353728099986d]<img width="2560" height="1369" alt="2025-08-03_21 09 33" src="https://github.com/user-attachments/assets/53df3ccd-4377-49ce-84b7-08ab24ee2930" />

拿著取得的數值填入 /autoaim position x y z 


