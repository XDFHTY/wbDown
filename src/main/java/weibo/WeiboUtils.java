package weibo;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import weibo.domain.VoUrlAndName;

@SuppressWarnings("deprecation")
public class WeiboUtils {
    public static boolean needFilterDate = false;
    public static String startTime;
    public static String endTime;
    /**
     * UA
     */
    static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Safari/537.36";

    /**
     * uidתcontianerId
     *
     * @author yanximin
     */
    static String uidToContainerId(String uid) {
        if (uid == null)
            throw new IllegalArgumentException("uid is null");
        return 107603 + uid;
    }

    /**
     * 昵称转ContainerId
     *
     * @throws IOException
     * @throws ClientProtocolException
     * @author yanximin
     */
    static String nicknameToContainerId(String nickname) throws ClientProtocolException, IOException {
        String url = "http://m.weibo.com/n/" + nickname;
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost post = new HttpPost(url);
        post.setHeader("User-Agent", USER_AGENT);
        HttpResponse response = httpClient.execute(post);
        post.abort();
        if (response.getStatusLine().getStatusCode() == 302) {
            String cid = response.getLastHeader("Location").getValue().substring(27);
            return "107603" + cid;
        }
        return null;
    }

    /**
     * 用户名转contianerId
     *
     * @throws IOException
     * @throws ClientProtocolException
     * @author yanximin
     */
    static String usernameToContainerId(String name) throws ClientProtocolException, IOException {
        String url = "https://weibo.cn/" + name;
        HttpClient httpClient = HttpClients.createDefault();
        HttpGet get = new HttpGet(url);
        get.setHeader("User-Agent", USER_AGENT);
        HttpResponse response = httpClient.execute(get);
        String ret = EntityUtils.toString(response.getEntity(), "utf-8");
        Pattern pattern = Pattern.compile("href=\"/([\\d]*?)/info\"");
        Matcher matcher = pattern.matcher(ret);
        while (matcher.find()) {
            return "107603" + matcher.group(1);
        }
        return null;
    }

    public static List<VoUrlAndName> getAllImgURL(String containerid) throws Exception {
//        List<String> urls = new ArrayList<String>();
        List<VoUrlAndName> voUrlAndNames = new ArrayList<>();
        int i = 1;
        while (getImgURL(containerid, i, voUrlAndNames) > 0) {
            System.out.println("分析微博中: " + i);
            i++;
            // 防封，分析一次页面休息+1S
            Thread.sleep(1000);
        }
        return voUrlAndNames;
    }

    private static String regEx = "<[^>]*>";
    private static String regEx2 = "[\\\\/:*?\"<>|]";

    private static String regEx3 = "[\ud83c\udc00-\ud83c\udfff]|[\ud83d\udc00-\ud83d\udfff]|[\u2600-\u27ff]";

    private static int getImgURL(String containerid, int page, List<VoUrlAndName> voUrlAndNames) throws ParseException, IOException {
        String url = "https://m.weibo.cn/api/container/getIndex?count=25&page=" + page + "&containerid=" + containerid;
        System.out.println(url);
        HttpClient httpClient = getHttpClient();
        HttpGet get = new HttpGet(url);
        get.setHeader("User-Agent", USER_AGENT);
        HttpResponse response = httpClient.execute(get);
        String ret = EntityUtils.toString(response.getEntity(), "utf-8");
        JsonObject root;
        try {
            // 防封
            root = new JsonParser().parse(ret).getAsJsonObject();
        } catch (Exception e) {
            try {
                Thread.sleep(60000);
            } catch (Exception e1) {

            }
            return 1;
        }
        JsonObject asJsonObject = root.getAsJsonObject("data");
        JsonArray array = asJsonObject.getAsJsonArray("cards");
        for (int i = 0; i < array.size(); i++) {
            JsonObject mblog = array.get(i).getAsJsonObject().getAsJsonObject("mblog");
            if (mblog != null) {
                String createAt = mblog.get("created_at").getAsString();
                String date0 = WeiboUtils.getDate(createAt);
                long time0 = WeiboUtils.getTime0(createAt);

                String source = mblog.get("source").getAsString();
                String text = mblog.get("text").getAsString().replaceAll(" ", "");
                String newText = text.replaceAll(regEx, "").replaceAll(regEx2, "").replaceAll(regEx3, "");
                if (newText.length() > 50) {
                    newText = newText.substring(0, 49) + "...";
                }
////                System.out.println("==>>    " + source);
//                System.out.println("==>>   " + date + "-" + source);
//
////                JsonObject pageInfo = mblog.getAsJsonObject("page_info");
////                if (pageInfo != null) {
////                    String title = pageInfo.get("page_title").getAsString();
////
////                }

                if (WeiboUtils.needFilterDate) {
                    long time1 =  WeiboUtils.getTime(WeiboUtils.startTime);
                    long time2 =  WeiboUtils.getTime(WeiboUtils.endTime);
                    System.out.println("t0:"+time0+"|t1:"+time1+"<> t2:"+time2);

                    if (time0 < time1|| time0 > time2) {
                        System.out.println("时间超出(" + WeiboUtils.startTime + "-" + WeiboUtils.endTime + "): " + date0);
                        continue;
                    }
                }
                JsonArray pics = mblog.getAsJsonArray("pics");
                if (pics != null) {
                    for (int j = 0; j < pics.size(); j++) {
                        JsonObject o = pics.get(j).getAsJsonObject();
                        JsonObject large = o.getAsJsonObject("large");
                        if (large != null) {
                            String fileUrl = large.get("url").getAsString();
                            VoUrlAndName voUrlAndName = new VoUrlAndName();
                            voUrlAndName.setUrl(fileUrl);
                            voUrlAndName.setFilename(date0 + "-" + (j + 1) + "-(" + newText + ")" + getSuffix(fileUrl));
                            voUrlAndNames.add(voUrlAndName);
//                            urls.add(large.get("url").getAsString());
                        }
                    }
                }
            }
        }
        return array.size();
    }

    /**
     * 初始HttpClient
     *
     * @author yanximin
     */
    public static HttpClient getHttpClient() {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 30000);
        httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 30000);
        return httpClient;
    }

    public static long transWeiboDateStrToTimeStamp(String weiboDateStr) {
        if (weiboDateStr == null || "".equals(weiboDateStr)) {
            return 0;
        }
        if (weiboDateStr.contains("秒前")) {
            weiboDateStr = weiboDateStr.replace("秒前", "");
            int second = Integer.valueOf(weiboDateStr);
            return System.currentTimeMillis() - second * 1000;
        }
        if (weiboDateStr.contains("分钟前")) {
            weiboDateStr = weiboDateStr.replace("分钟前", "");
            int second = Integer.valueOf(weiboDateStr);
            return System.currentTimeMillis() - second * 1000 * 60;
        }
        if (weiboDateStr.contains("小时前")) {
            weiboDateStr = weiboDateStr.replace("小时前", "");
            int second = Integer.valueOf(weiboDateStr);
            return System.currentTimeMillis() - second * 1000 * 3600;
        }
        if (weiboDateStr.contains("昨天")) {
            Date yesterdayTimestamp = new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
            String yesterday = simpleDateFormat.format(yesterdayTimestamp);
            weiboDateStr = weiboDateStr.replace("昨天", yesterday);
            SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyyMMdd HH:mm");
            try {
                Date date = sDateFormat.parse(weiboDateStr);
                return date.getTime();
            } catch (Exception e) {
                return 0;
            }
        }
        if (weiboDateStr.contains("-")) {
            if (!weiboDateStr.startsWith("20")) {
                int year = new Date().getYear() + 1900;
                weiboDateStr = year + "-" + weiboDateStr;
            }
            SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            try {
                Date date = sDateFormat.parse(weiboDateStr);
                return date.getTime();
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    public static String getDate(String weiboDateStr) {
//        long time;
//        try {
//            time = WeiboUtils.transWeiboDateStrToTimeStamp(weiboDateStr);
//        } catch (Exception e) {
//            time = 0;
//        }
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
        Date date = new Date();
        try {
            date = sdf.parse(weiboDateStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String formatStr = new SimpleDateFormat("yyyyMMdd-HHmmss").format(date);
//        System.err.println(formatStr);

        return formatStr;

//        SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd");
//        return sDateFormat.format(new Date(time));
    }
    public static String getDateStr(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date());
    }

    public static long getTime0(String dateStr) {
        long time;
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
        try {
            time = sdf.parse(dateStr).getTime();
        } catch (Exception e) {
            time = 0;
        }

        return time;
    }
    public static long getTime(String dateStr) {
        long time;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            time = sdf.parse(dateStr).getTime();
        } catch (Exception e) {
            time = 0;
        }

        return time;
    }

    private static String getSuffix(String url) {
        if (!url.substring(url.lastIndexOf("/")).contains(".")) {
            return ".jpg";
        }
        try {
            return url.substring(url.lastIndexOf("."));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ".jpg";
    }
}
