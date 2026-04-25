package com.example.walnex.auth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class CountryRepository {

    private static volatile List<Country> CACHE;

    private CountryRepository() {
        // Utility class.
    }

    public static List<Country> all() {
        if (CACHE == null) {
            synchronized (CountryRepository.class) {
                if (CACHE == null) {
                    List<Country> list = build();
                    Collections.sort(list, new Comparator<Country>() {
                        @Override
                        public int compare(Country a, Country b) {
                            return a.name.compareToIgnoreCase(b.name);
                        }
                    });
                    CACHE = Collections.unmodifiableList(list);
                }
            }
        }
        return CACHE;
    }

    public static int indexOfDialCode(String dialCode) {
        if (dialCode == null) {
            return -1;
        }
        List<Country> list = all();
        for (int i = 0; i < list.size(); i++) {
            if (dialCode.equals(list.get(i).dialCode)) {
                return i;
            }
        }
        return -1;
    }

    public static int indexOfIso2(String iso2) {
        if (iso2 == null) {
            return -1;
        }
        List<Country> list = all();
        for (int i = 0; i < list.size(); i++) {
            if (iso2.equalsIgnoreCase(list.get(i).iso2)) {
                return i;
            }
        }
        return -1;
    }

    private static List<Country> build() {
        List<Country> list = new ArrayList<>(220);
        list.add(new Country("Afghanistan", "AF", "+93"));
        list.add(new Country("Albania", "AL", "+355"));
        list.add(new Country("Algeria", "DZ", "+213"));
        list.add(new Country("Andorra", "AD", "+376"));
        list.add(new Country("Angola", "AO", "+244"));
        list.add(new Country("Antigua and Barbuda", "AG", "+1268"));
        list.add(new Country("Argentina", "AR", "+54"));
        list.add(new Country("Armenia", "AM", "+374"));
        list.add(new Country("Aruba", "AW", "+297"));
        list.add(new Country("Australia", "AU", "+61"));
        list.add(new Country("Austria", "AT", "+43"));
        list.add(new Country("Azerbaijan", "AZ", "+994"));
        list.add(new Country("Bahamas", "BS", "+1242"));
        list.add(new Country("Bahrain", "BH", "+973"));
        list.add(new Country("Bangladesh", "BD", "+880"));
        list.add(new Country("Barbados", "BB", "+1246"));
        list.add(new Country("Belarus", "BY", "+375"));
        list.add(new Country("Belgium", "BE", "+32"));
        list.add(new Country("Belize", "BZ", "+501"));
        list.add(new Country("Benin", "BJ", "+229"));
        list.add(new Country("Bhutan", "BT", "+975"));
        list.add(new Country("Bolivia", "BO", "+591"));
        list.add(new Country("Bosnia and Herzegovina", "BA", "+387"));
        list.add(new Country("Botswana", "BW", "+267"));
        list.add(new Country("Brazil", "BR", "+55"));
        list.add(new Country("Brunei", "BN", "+673"));
        list.add(new Country("Bulgaria", "BG", "+359"));
        list.add(new Country("Burkina Faso", "BF", "+226"));
        list.add(new Country("Burundi", "BI", "+257"));
        list.add(new Country("Cambodia", "KH", "+855"));
        list.add(new Country("Cameroon", "CM", "+237"));
        list.add(new Country("Canada", "CA", "+1"));
        list.add(new Country("Cape Verde", "CV", "+238"));
        list.add(new Country("Central African Republic", "CF", "+236"));
        list.add(new Country("Chad", "TD", "+235"));
        list.add(new Country("Chile", "CL", "+56"));
        list.add(new Country("China", "CN", "+86"));
        list.add(new Country("Colombia", "CO", "+57"));
        list.add(new Country("Comoros", "KM", "+269"));
        list.add(new Country("Congo", "CG", "+242"));
        list.add(new Country("Congo (DRC)", "CD", "+243"));
        list.add(new Country("Costa Rica", "CR", "+506"));
        list.add(new Country("Côte d'Ivoire", "CI", "+225"));
        list.add(new Country("Croatia", "HR", "+385"));
        list.add(new Country("Cuba", "CU", "+53"));
        list.add(new Country("Cyprus", "CY", "+357"));
        list.add(new Country("Czech Republic", "CZ", "+420"));
        list.add(new Country("Denmark", "DK", "+45"));
        list.add(new Country("Djibouti", "DJ", "+253"));
        list.add(new Country("Dominica", "DM", "+1767"));
        list.add(new Country("Dominican Republic", "DO", "+1809"));
        list.add(new Country("Ecuador", "EC", "+593"));
        list.add(new Country("Egypt", "EG", "+20"));
        list.add(new Country("El Salvador", "SV", "+503"));
        list.add(new Country("Equatorial Guinea", "GQ", "+240"));
        list.add(new Country("Eritrea", "ER", "+291"));
        list.add(new Country("Estonia", "EE", "+372"));
        list.add(new Country("Eswatini", "SZ", "+268"));
        list.add(new Country("Ethiopia", "ET", "+251"));
        list.add(new Country("Fiji", "FJ", "+679"));
        list.add(new Country("Finland", "FI", "+358"));
        list.add(new Country("France", "FR", "+33"));
        list.add(new Country("Gabon", "GA", "+241"));
        list.add(new Country("Gambia", "GM", "+220"));
        list.add(new Country("Georgia", "GE", "+995"));
        list.add(new Country("Germany", "DE", "+49"));
        list.add(new Country("Ghana", "GH", "+233"));
        list.add(new Country("Greece", "GR", "+30"));
        list.add(new Country("Grenada", "GD", "+1473"));
        list.add(new Country("Guatemala", "GT", "+502"));
        list.add(new Country("Guinea", "GN", "+224"));
        list.add(new Country("Guinea-Bissau", "GW", "+245"));
        list.add(new Country("Guyana", "GY", "+592"));
        list.add(new Country("Haiti", "HT", "+509"));
        list.add(new Country("Honduras", "HN", "+504"));
        list.add(new Country("Hong Kong", "HK", "+852"));
        list.add(new Country("Hungary", "HU", "+36"));
        list.add(new Country("Iceland", "IS", "+354"));
        list.add(new Country("India", "IN", "+91"));
        list.add(new Country("Indonesia", "ID", "+62"));
        list.add(new Country("Iran", "IR", "+98"));
        list.add(new Country("Iraq", "IQ", "+964"));
        list.add(new Country("Ireland", "IE", "+353"));
        list.add(new Country("Israel", "IL", "+972"));
        list.add(new Country("Italy", "IT", "+39"));
        list.add(new Country("Jamaica", "JM", "+1876"));
        list.add(new Country("Japan", "JP", "+81"));
        list.add(new Country("Jordan", "JO", "+962"));
        list.add(new Country("Kazakhstan", "KZ", "+7"));
        list.add(new Country("Kenya", "KE", "+254"));
        list.add(new Country("Kiribati", "KI", "+686"));
        list.add(new Country("Kuwait", "KW", "+965"));
        list.add(new Country("Kyrgyzstan", "KG", "+996"));
        list.add(new Country("Laos", "LA", "+856"));
        list.add(new Country("Latvia", "LV", "+371"));
        list.add(new Country("Lebanon", "LB", "+961"));
        list.add(new Country("Lesotho", "LS", "+266"));
        list.add(new Country("Liberia", "LR", "+231"));
        list.add(new Country("Libya", "LY", "+218"));
        list.add(new Country("Liechtenstein", "LI", "+423"));
        list.add(new Country("Lithuania", "LT", "+370"));
        list.add(new Country("Luxembourg", "LU", "+352"));
        list.add(new Country("Macao", "MO", "+853"));
        list.add(new Country("Madagascar", "MG", "+261"));
        list.add(new Country("Malawi", "MW", "+265"));
        list.add(new Country("Malaysia", "MY", "+60"));
        list.add(new Country("Maldives", "MV", "+960"));
        list.add(new Country("Mali", "ML", "+223"));
        list.add(new Country("Malta", "MT", "+356"));
        list.add(new Country("Marshall Islands", "MH", "+692"));
        list.add(new Country("Mauritania", "MR", "+222"));
        list.add(new Country("Mauritius", "MU", "+230"));
        list.add(new Country("Mexico", "MX", "+52"));
        list.add(new Country("Micronesia", "FM", "+691"));
        list.add(new Country("Moldova", "MD", "+373"));
        list.add(new Country("Monaco", "MC", "+377"));
        list.add(new Country("Mongolia", "MN", "+976"));
        list.add(new Country("Montenegro", "ME", "+382"));
        list.add(new Country("Morocco", "MA", "+212"));
        list.add(new Country("Mozambique", "MZ", "+258"));
        list.add(new Country("Myanmar", "MM", "+95"));
        list.add(new Country("Namibia", "NA", "+264"));
        list.add(new Country("Nauru", "NR", "+674"));
        list.add(new Country("Nepal", "NP", "+977"));
        list.add(new Country("Netherlands", "NL", "+31"));
        list.add(new Country("New Zealand", "NZ", "+64"));
        list.add(new Country("Nicaragua", "NI", "+505"));
        list.add(new Country("Niger", "NE", "+227"));
        list.add(new Country("Nigeria", "NG", "+234"));
        list.add(new Country("North Korea", "KP", "+850"));
        list.add(new Country("North Macedonia", "MK", "+389"));
        list.add(new Country("Norway", "NO", "+47"));
        list.add(new Country("Oman", "OM", "+968"));
        list.add(new Country("Pakistan", "PK", "+92"));
        list.add(new Country("Palau", "PW", "+680"));
        list.add(new Country("Palestine", "PS", "+970"));
        list.add(new Country("Panama", "PA", "+507"));
        list.add(new Country("Papua New Guinea", "PG", "+675"));
        list.add(new Country("Paraguay", "PY", "+595"));
        list.add(new Country("Peru", "PE", "+51"));
        list.add(new Country("Philippines", "PH", "+63"));
        list.add(new Country("Poland", "PL", "+48"));
        list.add(new Country("Portugal", "PT", "+351"));
        list.add(new Country("Puerto Rico", "PR", "+1787"));
        list.add(new Country("Qatar", "QA", "+974"));
        list.add(new Country("Romania", "RO", "+40"));
        list.add(new Country("Russia", "RU", "+7"));
        list.add(new Country("Rwanda", "RW", "+250"));
        list.add(new Country("Saint Kitts and Nevis", "KN", "+1869"));
        list.add(new Country("Saint Lucia", "LC", "+1758"));
        list.add(new Country("Saint Vincent and the Grenadines", "VC", "+1784"));
        list.add(new Country("Samoa", "WS", "+685"));
        list.add(new Country("San Marino", "SM", "+378"));
        list.add(new Country("Sao Tome and Principe", "ST", "+239"));
        list.add(new Country("Saudi Arabia", "SA", "+966"));
        list.add(new Country("Senegal", "SN", "+221"));
        list.add(new Country("Serbia", "RS", "+381"));
        list.add(new Country("Seychelles", "SC", "+248"));
        list.add(new Country("Sierra Leone", "SL", "+232"));
        list.add(new Country("Singapore", "SG", "+65"));
        list.add(new Country("Slovakia", "SK", "+421"));
        list.add(new Country("Slovenia", "SI", "+386"));
        list.add(new Country("Solomon Islands", "SB", "+677"));
        list.add(new Country("Somalia", "SO", "+252"));
        list.add(new Country("South Africa", "ZA", "+27"));
        list.add(new Country("South Korea", "KR", "+82"));
        list.add(new Country("South Sudan", "SS", "+211"));
        list.add(new Country("Spain", "ES", "+34"));
        list.add(new Country("Sri Lanka", "LK", "+94"));
        list.add(new Country("Sudan", "SD", "+249"));
        list.add(new Country("Suriname", "SR", "+597"));
        list.add(new Country("Sweden", "SE", "+46"));
        list.add(new Country("Switzerland", "CH", "+41"));
        list.add(new Country("Syria", "SY", "+963"));
        list.add(new Country("Taiwan", "TW", "+886"));
        list.add(new Country("Tajikistan", "TJ", "+992"));
        list.add(new Country("Tanzania", "TZ", "+255"));
        list.add(new Country("Thailand", "TH", "+66"));
        list.add(new Country("Timor-Leste", "TL", "+670"));
        list.add(new Country("Togo", "TG", "+228"));
        list.add(new Country("Tonga", "TO", "+676"));
        list.add(new Country("Trinidad and Tobago", "TT", "+1868"));
        list.add(new Country("Tunisia", "TN", "+216"));
        list.add(new Country("Türkiye", "TR", "+90"));
        list.add(new Country("Turkmenistan", "TM", "+993"));
        list.add(new Country("Tuvalu", "TV", "+688"));
        list.add(new Country("Uganda", "UG", "+256"));
        list.add(new Country("Ukraine", "UA", "+380"));
        list.add(new Country("United Arab Emirates", "AE", "+971"));
        list.add(new Country("United Kingdom", "GB", "+44"));
        list.add(new Country("United States", "US", "+1"));
        list.add(new Country("Uruguay", "UY", "+598"));
        list.add(new Country("Uzbekistan", "UZ", "+998"));
        list.add(new Country("Vanuatu", "VU", "+678"));
        list.add(new Country("Vatican City", "VA", "+379"));
        list.add(new Country("Venezuela", "VE", "+58"));
        list.add(new Country("Vietnam", "VN", "+84"));
        list.add(new Country("Yemen", "YE", "+967"));
        list.add(new Country("Zambia", "ZM", "+260"));
        list.add(new Country("Zimbabwe", "ZW", "+263"));
        return list;
    }
}
