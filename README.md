brigen fx-player 「Narudake Player」
=============


JavaFX（ジャバえふえっくす）で書いた動画や音声をなるだけ再生する Player です

ムシャクシャして作った。公開はしている


最新版
-------

2.7.2


#### 機能

* それなりに動画も再生できるようになりました（動画タブで切り替え）
* Supported Media Codecs - [Oracleのサイト][1]

[1]: http://docs.oracle.com/javafx/2/media/overview.htm

  
  
* Nic○nic○の検索結果でもなるだけ再生するようにしました（テキストボックスに「nc=検索ワード」と入力してEnter。次ページ自動遷移あり。）
* Nic○nic○の場合はアカウントの入力が必要です。詳しくはショートカットのソースからそれっぽいところを見てください。（自己責任でおながいします）
* Nic○nic○の /search /mylist のURLでもなるだけ再生するようにしました
* Nic○nic○の /search のURLの場合はリストを最後まで行くと自動で次ページを取得します
* Y○utubeの /playlist /channel /user /results のURLでもなるだけ再生するようにしました
* Y○utubeの /results のURLの場合はリストを最後まで行くと自動で次ページを取得します
* Y○utubeの検索結果でもなるだけ再生するようにしました（テキストボックスに「yt=検索ワード」と入力してEnter。次ページ自動遷移あり。）

* Y○utubeの動画のURLを記述したプレイリストのファイルでもなるだけ再生するようにしました
* Xvide○sの動画のURLを記述したプレイリストのファイルでもなるだけ再生しようとしますが、対応してないフォーマットなので再生できません('A`)
  
  

* プレイリストのファイルを順に再生します。停止、スキップ、ジャンプ（操作：リストの項目をダボォクリック!）もできます
* 再生位置の変更（シーク）、音量変更、（タグ情報の）画像の表示と保存（操作：画像をダボォクリック!!）ができます
* MP3のタグ情報または、FLVの情報をもとに表示されるので、この情報がない場合はちょっと残念です（この場合でもMP3は再生はできます）
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
4. MP3のタグ情報または、FLVの情報を読み取ったらその情報を表示します（震えて待て）
5. 「 ||> 」を非ダボォクリックしてみよう
6. (ﾟ∀ﾟ)b ゆっくりきいてってね！！！

#### プレイリストを読み込み（＊１）

* フォルダから

「フォルダ」からフォルダを選択すると、そのフォルダ内のMP3ファイルをプレイリストとして読み込みます（フォルダを再帰的には読みません）

* プレイリストファイルから

「リスト」からテキストファイルを選択すると、そのファイルの内容をプレイリストファイルとして読み込みます

* プレイリストファイルのURLを指定

テキストボックスにURL（http://～に限る）を直接入力（してenter）すると、プレイリストファイルをhttpから取得します

* Y○utubeのURLを指定

テキストボックスにY○utubeの /playlist /channel /user /results のURLを直接入力（してenter）すると、そのページの動画をプレイリストとして読み込みます

* Y○utubeの検索

テキストボックスに「yt=検索ワード」と入力（してenter）すると、その検索結果の動画をプレイリストとして読み込みます。
プレイリスト（20件）の最後まで再生すると自動で次のページを取得して再生を続けます


#### プレイリストファイル

* たぶん、エンコードはUTF-8がいいとおもいます
* ローカルのプレイリストファイルのサンプル - sample/local_playlist.txt
* httpから取得するプレイリストファイルのサンプル - 仕様の変更により、一時的にフォーマットが未確定です



その他機能
-------

#### ショートカット

* ShortcutHandlerとかいうクラスのソース参照（com.brightgenerous.fxplayer.application.playlist.ShortcutHandler.java）

#### 内部的なやつ

* キャッシュ - なるだけがんばってる
* 遅延ロード - なるだけがんばってる
* リンク切れリロード - なるだけがんばってる


ビルド
-------

環境に合わせて pom.xml ファイルの65行目あたりを書き換えてください

      <systemPath>C:\Program Files\Java\jre7\lib\jfxrt.jar</systemPath>


contributors
-------

* BrightGenerous - [twitter][2]



[2]: https://twitter.com/BrightGenerous
