import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

// jbr-11 사용됨
// 이하 두개의 라이브러리 필수적
// io.github.bonigarcia.webdrivermanager
// seleniumhq.selenium.java
// maven으로 가져옴...

public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
        Scanner scanner = new Scanner(System.in);

        // jlpt N1~N5 중 선택
        System.out.println("Select jlpt Lv (1/2/3/4/5)");
        int jlptLevel = scanner.nextInt();
        if (jlptLevel != 1 && jlptLevel != 2 && jlptLevel != 3 && jlptLevel != 4 && jlptLevel != 5) {
            System.out.println("Select correct Lv");
            return;
        }
        System.out.println("jlpt N" + jlptLevel + " selected");

        // 폴더 없으면 생성 (C:/에 생성됨)
        File path = new File("/jlptword/");
        if (!path.exists()) {
            path.mkdir();
        }

        // json 파일 생성, 파일 이미 있는 경우 확인
        File file = new File(path, "JLPT_N" + jlptLevel + ".json");
        if (file.exists()) {
            System.out.println("File already exists, Continue? (Y/N)");
            String continueOption = scanner.next();
            if (continueOption.equals("Y") || continueOption.equals("y")) {
                System.out.println("Yes Selected, Continue...");
                file.createNewFile();
            } else if (continueOption.equals("N") || continueOption.equals("n")) {
                System.out.println("No Selected, Stop.");
                return;
            } else {
                System.out.println("Select Y or N, Stop.");
                return;
            }
        }

        // 크롬웹드라이버 실행
        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();

        // 파일에 대해 BufferedWriter 시작
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write("[\n");

        // 시작 페이지, 끝 페이지 지정 (가장 단어가 많은 N1의 최대 페이지 편의상 325로 하드코딩함)
        int min_pagenum = 1;
        int max_pagenum = 325;

        // 크롤링해서 하나씩 파일에 집어넣음...xpath로 경로 지정함
        for (int pagenum = min_pagenum; pagenum <= max_pagenum; pagenum++) {
            driver.get("https://ja.dict.naver.com/#/jlpt/list?level=" + jlptLevel + "&part=allClass&page=" + pagenum);
            Thread.sleep(300);
            for (int i = 1; i <= 10; i++) {
                try {
                    String xpath = "//*[@id=\"my_jlpt_list_template\"]/li[" + i + "]";
                    String pronunciation = driver.findElement(By.xpath(xpath + "/div/a")).getText();
                    String kanji = driver.findElement(By.xpath(xpath + "/div/span[1]")).getText();
                    if (kanji.contains("듣기")) // 한자 없는 경우 "듣기" 또는 "발음듣기"로 가져와짐
                        kanji = pronunciation; // 이와 같은 경우 한자 <- 발음으로 대체
                    String meaning = driver.findElement(By.xpath(xpath + "/ul/li/p")).getText();
                    writer.write("{\n\t\"pronunciation\": \"" + pronunciation + "\",\n\t\"kanji\": \"" + kanji + "\",\n\t\"meaning\": \"" + meaning + "\",\n},\n");
                } catch (NoSuchElementException e) {
                    System.out.println("Crawling Complete(or error point) - [page / i] = [ " + pagenum + " / " + (i - 1) + " ]");
                    writer.write("]");
                    writer.close();
                    return;
                }
            }
        }
        // 혹시 쓸까봐 추가했는데 별로 의미는 없음...시작/끝 페이지 지정시에만 의미있을듯...
        writer.write("]");
        writer.close();
    }
}