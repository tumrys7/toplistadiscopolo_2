package com.grandline.toplistadiscopolo;

public interface Constants {

	//PRO or FREE Version constant
	//free version has advertisement, has no votes showing
	//public boolean VERSION_PRO_SHOW_VOTES = false;
    boolean VERSION_PRO_DO_NOT_SHOW_BANNER = false;
	boolean VERSION_PRO_SHOW_VOTES = false;
	// All public variables
	//groupid=3 - Disco Polo
    String URL = "http://toplista.gline.webd.pl/gen_xml_lista_mobile.php?lang=LANG&groupid=3";
	String URL_NOWOSCI = "http://toplista.gline.webd.pl/gen_xml_nowosci_mobile.php?lang=LANG&groupid=3";
	String URL_MOJALISTA = "http://toplista.gline.webd.pl/gen_xml_mojalista_mobile.php?lang=LANG&groupid=3&android_id=ANDROIDID";
	String URL_NOTOWANIA = "http://toplista.gline.webd.pl/gen_xml_notowania_mobile.php?lang=LANG&groupid=3&notowanieid=START_NOTOWANIE_ID";
    String VOTE_URL = "http://toplista.gline.webd.pl/index_andro.php?glos=ID_LISTY&aid=DEV_ID&lang=LANG&groupid=3&teledysk=TEL_PARAM";
	String WYK_URL = "http://toplista.gline.webd.pl/gen_xml_wyk.php?groupid=3";
	String UTWORY_WYK_URL = "http://toplista.gline.webd.pl/gen_xml_utw_wyk_mobile.php?wyk_id=AUTH_ID&groupid=3";
	String FACEBOOK_URL = "https://www.facebook.com/discopololista";
	String POLICY_URL = "https://www.google.com/about/company/user-consent-policy.html";

	String VALUE_START_NOTOWANIE_ID = "START_NOTOWANIE_ID";

	
	// XML node keys
	String KEY_INFO = "info";
	String KEY_LISTA = "lista";
	String KEY_MOJALISTA = "mojalista";
	String KEY_POCZEKALNIA = "poczekalnia";
	String KEY_NOWOSCI = "nowosci";
	String KEY_LISTA_NOTOWANIA = "notowania";
	String KEY_UTW_WYKONAWCY = "utw_wykonawcy";
	String KEY_SONG = "rec"; // parent node for list
	String KEY_SONG_POCZ = "recp"; // parent node for poczekalnia
	String KEY_SONG_NOWOSCI="recnew"; // parent node for nowosci
	String KEY_SONG_MOJA="recmoja"; // parent node for moja
	String KEY_ID = "id_listy";
	String KEY_ID_GRUPY = "id_grupy";
	String KEY_TITLE = "tytul";
	String KEY_ARTIST = "autor";
	String KEY_ARTIST_ID = "wyk_id";
	String KEY_VOTES = "ile_glosow";
	String KEY_THUMB_URL = "okladka";
	String KEY_CREATE_DATE = "data_dodania";
	String KEY_POSITION = "miejsce";
	String KEY_VIDEO = "youtube";
	String KEY_SPOTIFY = "spotify";
	//old public final String KEY_AD_UNIT_ID = "ca-app-pub-9534304538332188/9715986154";
  //  String KEY_AD_UNIT_ID = "ca-app-pub-9534304538332188/7239850955";
	String KEY_FULLAD_UNIT_ID = "ca-app-pub-9534304538332188/4908744158";
	String KEY_NATIVEAD_UNIT_ID = "ca-app-pub-9534304538332188/7039312538";
	String KEY_VIDEO_UNIT_ID ="ca-app-pub-9534304538332188/8329857998";
	String KEY_ADMOB_APP_ID = "ca-app-pub-9534304538332188~6762519752";
	String KEY_WYKONAWCY = "wykonawcy";
	String KEY_WYKON = "recw"; // parent node for wykonawcy
	String KEY_ID_WYKON = "id_wykonawcy";
	String KEY_WYKONAWCA = "nazwa";
	String KEY_PLACE_CHANGE = "zmiana";
	
	String KEY_NOTOWANIE_PRZEDZIAL = "przedzial";
	String KEY_NOTOWANIE_ZA = "notowanie_za";
	String KEY_NOTOWANIE_NAZWA = "nazwa_notowania";
	
	//arrow types
	String KEY_ARROW_TYPE = "arrow_type";
	String KEY_ARROW_UP = "arrow_up";
	String KEY_ARROW_DOWN = "arrow_down";
	String KEY_ARROW_NO_CHANGE = "arrow_no_change";
	
	String KEY_VOTES_PROGRESS = "votes_progress";
	String KEY_SHOW_VOTES_PROGRESS = "show_votes_progress";
	
	//e-mail constants
    String EMAIL_TO = "discopololista@gline.webd.pl";

	//votes interval in minutes (for the same song)
	int VOTES_INTERVAL = 5;
	
	//informations
	String TEXT_VOTE_INFO="<p class=\"INFO\">";
	String TEXT_VOTE_ERROR="<p class=\"ERROR\">";
	
	//text separator between key values
	String TEXT_SEPARATOR = " | ";
	
	String PREFS_NAME = "PrefsFile";
	
}
