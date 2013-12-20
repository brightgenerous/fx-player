brigen fx-player 「Narudake Player」
=============


JavaFX（ジャバえふえっくす）で書いた動画や音声をなるだけ再生する Player です

ムシャクシャして作った。公開はしている


最新版
-------

2.1.1


#### 機能

* それなりに動画も再生できるようになりました（動画タブで切り替え）

Supported Media Codecs - [Oracleのサイト][1]

[1]: http://docs.oracle.com/javafx/2/media/overview.htm


* プレイリストのファイルを順に再生します。停止、スキップ、ジャンプ（操作：リストの項目をダボォクリック!）もできます
* 再生位置の変更（シーク）、音量変更、（タグ情報の）画像の表示と保存（操作：画像をダボォクリック!!）ができます
* MP3のタグ情報をもとに表示されるので、タグ情報がない場合は残念です（この場合でも再生はできます）
* タグ情報はツールチップでも表示されます
* プレイリストは「ローカルマシンのフォルダにあるファイル一覧」、または、「プレイリストファイル」から読み込みます
* プレイリストファイルは「ローカルマシンのプレイリストファイル」、または、「プレイリストファイルのURL」から指定できます
* プレイリストファイル内の記述（URL指定）でファイルを指定する場合は「https://~」が使えません
* ログウィンドウでログも見れます。気になる場合は使ってください


使い方
-------

* build/fx-player.jar - このファイルがそのまま実行可能です（要：なるだけ最新JRE 7）

1. build/fx-player.jarをダボォクリック
2. アプリが起動しましたね
3. プレイリストを読み込み（＊１）
4. MP3のタグ/FLVの情報を読み取ったらその情報を表示します（震えて待て）
5. 「再生／停止」を非ダボォクリックしてみよう
6. (ﾟ∀ﾟ)b ゆっくりきいてってね！！！

#### プレイリストを読み込み（＊１）

* フォルダから

「フォルダ」からフォルダを選択すると、そのフォルダ内のMP3ファイルをプレイリストとして読み込みます。（フォルダを再帰的には読みません）

* プレイリストファイルから

「リスト」からテキストファイルを選択すると、そのファイルの内容をプレイリストファイルとして読み込みます。

* プレイリストファイルのURLを指定

テキストボックスにURL（http://～に限る）を直接入力（してenter）すると、プレイリストファイルをhttpから取得します。

#### プレイリストファイル

* たぶん、エンコードはUTF-8がいいとおもいます
* ローカルのプレイリストファイルのサンプル - sample/local_playlist.txt
* httpから取得するプレイリストファイルのサンプル - sample/url_playlist.txt



ビルド
-------

環境に合わせて pom.xml ファイルの65行目あたりを書き換えてください

      <systemPath>C:\Program Files\Java\jre7\lib\jfxrt.jar</systemPath>


contributors
-------

* BrightGenerous - [twitter][2]



[2]: https://twitter.com/BrightGenerous
