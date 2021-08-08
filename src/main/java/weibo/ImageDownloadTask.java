package weibo;

import java.util.concurrent.CountDownLatch;

public class ImageDownloadTask implements Runnable{
    private CountDownLatch downLatch;
    private int imageIndex;
    private String filename;
    private String imageUrl;
    
    public int getImageIndex() {
        return imageIndex;
    }
    public void setImageIndex(int imageIndex) {
        this.imageIndex = imageIndex;
    }
    public String getImageUrl() {
        return imageUrl;
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public ImageDownloadTask(CountDownLatch downLatch,int imageIndex, String filename, String imageUrl) {
        super();
        this.downLatch = downLatch;
        this.imageIndex = imageIndex;
        this.filename = filename;
        this.imageUrl = imageUrl;
    }

    @Override
    public void run() {
        try{
            System.out.println("下载图片: " + ( imageIndex + 1)+":"+filename);
            byte[] imgBytes = FileUtils.download(imageUrl, 100_000);
            FileUtils.byte2File(imgBytes, WeiboDownloader.IMG_LOCATION, filename);
        }catch (Exception e) {
        }finally {
            downLatch.countDown();
        }
    }

}
