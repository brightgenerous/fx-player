# brigen fx-player
======

JavaFXで書いたMP3 Playerです

ムシャクシャして作った。公開はしている


#### 最新版

1.1.1


------

### 使い方
* build/fx-player.jar - このファイルがそのまま実行可能です。

1. build/fx-player.jarをダボォクリック
2. アプリが起動しましたね
3. プレイリストを読み込み（＊１）
4. ファイルのタグを読み取れたら情報が表示されます（震えて待て）
5. 「再生／停止」を非ダボォクリックしてみよう
6. (ﾟ∀ﾟ)b ゆっくりきいてってね！！！

#### ＊１

* フォルダから

「フォルダ」からフォルダを選択すると、そのフォルダ内のファイルをプレイリストとして読み込みます。（フォルダを再帰的には読みません）

* リストファイルから

「リスト」からテキストファイルを選択すると、そのファイルの内容をプレイリストとして読み込みます。

* リストファイルのURLを指定

テキストボックスにURL（http://～に限る）を直接入力（してenter）すると、リストファイルをhttpから取得します。

#### リストファイル

* たぶん、エンコードはUTF-8がいいとおもいます
* 一行一ファイルに対応してます
* ローカルファイルのプレイリストのサンプル - sample/local_playlist.txt
* httpから取得したプレイリストのサンプル - sample/url_playlist.txt



------

### ビルド

pom.xmlファイルの59行目あたりを書き換えてください

      <systemPath>C:\Program Files\Java\jre7\lib\jfxrt.jar</systemPath>


