import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Front extends HttpServlet {

    private static final int n = 10000000;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        if (request.getRequestURI().equals("/")) createPage(response, request);
        else {
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().println("<a href=\"http://localhost:8080/\">На главную</a>");
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private void createPage(HttpServletResponse response, HttpServletRequest request) throws IOException {
        getFiles(request, response);
        String selected = request.getParameter("file_matrix");
        response.getWriter().println("<table>");
        for (int i=1; i<=n; i++) {
            response.getWriter().println("<tr>");
            for (int j = 1; j <= n; j++) {
                if (i==1 && j==1) response.getWriter().println("<td><input required style=\"width: 60px;\" max=\"100\" type=\"number\" name=\"m_" + i + "_" + j + "\"></td>");
                else response.getWriter().println("<td><input style=\"width: 60px;\" max=\"100\" type=\"number\" name=\"m_" + i + "_" + j + "\"></td>");
            }
            response.getWriter().println("</tr>");
        }
        response.getWriter().println("</form>");
    }

    private void getFiles (HttpServletRequest request, HttpServletResponse response) throws IOException {
        List<File> files;
        HashMap<String, Object> vars = new HashMap<>(0);
        response.getWriter().println(PageGenerator.instance().getPage("input.html", vars));
        response.getWriter().println("<select id=\"select\" name=\"file_matrix\"><option disabled>Выберите файл со статичной матрицей...</option>");
        files = Files.walk(Paths.get("matrices"))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .collect(Collectors.toList());
        files.forEach(fl -> {
            try {
                if (request.getParameter("file_matrix") != null && request.getParameter("file_matrix").equals(fl.getName())) {
                    response.getWriter().println("<option value=\"" + fl.getName() + "\" selected>" + fl.getName() + "</option>");
                } else
                    response.getWriter().println("<option value=\"" + fl.getName() + "\">" + fl.getName() + "</option>");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        response.getWriter().println("</select><br/>");
        response.getWriter().println("<input type=\"submit\" value=\"Отправить данные на сервер\"><br/>");
    }

    private Map<String, Double> matrix(HttpServletRequest request) {
        Map<String, Double> mat = new HashMap<>();
        String cell;
        int col, prev_col = 0;
        int row = 1;
        boolean rowNotNull;
        for (int i=1; i<=n; i++) {
            rowNotNull = false;
            col = 0;
            for (int j=1; j<=n; j++) {
                cell = request.getParameter("m_"+i+"_"+j);
                if (!cell.equals("")) {
                    col = j;
                    rowNotNull = true;
                    mat.put("m_"+i+"_"+j, Double.parseDouble(cell));
                }
            }
            if (prev_col!=col && col!=0 && i!=1) {
                mat.put("wrong", 1.0);
                return mat;
            } else if (col!=0) prev_col = col;
            if (rowNotNull) row=i;
        }
        mat.put("row", (double) row);
        mat.put("col", (double) prev_col);
        return mat;
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        Map<String, Double> matrix = matrix(request);
        if (matrix.containsKey("wrong")) {
            response.getWriter().println("<h3><b>Вы допустили ошибку при вводе матрицы</b></h3>");
        }
        else {
            int col = (int) Math.round(matrix.get("col"));
            int row = (int) Math.round(matrix.get("row"));
            Map<String, Double> mat = new HashMap<>();
            Map<String, Double> res;
            response.getWriter().println("Входная матрица введена верно<br/>");
            String which = request.getParameter("which_matrix");
            switch (which) {
                case ("gaussian"): {
                    res = gaussianRes(matrix, row);
                    if (res.containsKey("wrong")) {
                        response.getWriter().println("<h3><b>Не существует обратной матрицы</b></h3>");
                    }
                    else {
                        outResult(res, row, row);
                        response.getWriter().println("<h3><b>Операция успешна</b></h3>");
                    }
                    break;
                }
                case ("unit"): {
                    //При умножении матрицы на единичную матрицу всегда получится та же матрица
                    outResult(matrix, row, col);
                    response.getWriter().println("<h3><b>Операция успешна</b></h3>");
                    break;
                }
                case ("random"): {
                    res = randomRes(matrix, row, col);
                    outResult(res, row, (int) Math.round(res.get("mat_col")));
                    response.getWriter().println("<h3><b>Операция успешна</b></h3>");
                    break;
                }
                case ("static"): {
                    String selected = request.getParameter("file_matrix");
                    res = staticRes(selected, matrix, row, col);
                    if (res.containsKey("wrong")) {
                        if (Math.round(res.get("wrong"))==1) {
                            response.getWriter().println("<h3><b>Матрица в файле введена неверно</b></h3>");
                        } else {
                            response.getWriter().println("<h3><b>Количество столбцов входной матрицы не совпадает с количеством строк матрицы в файле<br/>Умножение не может быть произведено</b></h3>");
                        }
                    }
                    else {
                        outResult(res, col, (int) Math.round(res.get("col")));
                        response.getWriter().println("<h3><b>Операция успешна</b></h3>");
                    }
                    break;
                }
            }
        }
        createPage(response, request);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private Map<String, Double> gaussianRes(Map<String, Double> matrix, int row) {
        Map<String, Double> matrixBig = new HashMap<>();
        Map<String, Double> Matrix = new HashMap<>();
        for (int i=0; i<row; i++) {
            for (int j=0; j<row; j++) {
                matrixBig.put("m_"+i+"_"+j, matrix.get("m_"+(i+1)+"_"+(j+1)));
                if (i==j) matrixBig.put("m_"+i+"_"+(j+row), 1.0);
                else matrixBig.put("m_"+i+"_"+(j+row), 0.0);
            }
        }
        for (int k = 0; k < row; k++) //k-номер строки
        {
            for (int i = 0; i < 2*row; i++) //i-номер столбца
                matrixBig.put("m_"+k+"_"+i, matrixBig.get("m_"+k+"_"+i) / matrix.get("m_"+(k+1)+"_"+(k+1))); //Деление k-строки на первый член !=0 для преобразования его в единицу
            for (int i = k + 1; i < row; i++) //i-номер следующей строки после k
            {
                double K = matrixBig.get("m_"+i+"_"+k) / matrixBig.get("m_"+k+"_"+k); //Коэффициент
                for (int j = 0; j < 2*row; j++) //j-номер столбца следующей строки после k
                    matrixBig.put("m_"+i+"_"+j, matrixBig.get("m_"+i+"_"+j) - matrixBig.get("m_"+k+"_"+j) * K); //Зануление элементов матрицы ниже первого члена, преобразованного в единицу
            }
            for (int i = 0; i < row; i++) //Обновление, внесение изменений в начальную матрицу
                for (int j = 0; j < row; j++)
                    Matrix.put("m_"+i+"_"+j, matrixBig.get("m_"+i+"_"+j));
        }
        for (int k = row-1; k > -1; k--) //k-номер строки
        {
            for (int i = 2*row-1; i > -1; i--) //i-номер столбца
                matrixBig.put("m_"+k+"_"+i, matrixBig.get("m_"+k+"_"+i) / Matrix.get("m_"+k+"_"+k));
            for (int i = k-1; i > -1; i--) //i-номер следующей строки после k
            {
                double K = matrixBig.get("m_"+i+"_"+k) / matrixBig.get("m_"+k+"_"+k);
                for (int j = 2*row-1; j > -1; j--) //j-номер столбца следующей строки после k
                    matrixBig.put("m_"+i+"_"+j, matrixBig.get("m_"+i+"_"+j) - matrixBig.get("m_"+k+"_"+j) * K);
            }
        }
        Double present;
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < row; j++) {
                present = matrixBig.get("m_" + i + "_" + (j+row));
                if (present.isInfinite() || present.isNaN()) {
                    Matrix.put("wrong", 1.0);
                    return  Matrix;
                }
                Matrix.put("m_" + (i+1) + "_" + (j+1), (double) Math.round(matrixBig.get("m_" + i + "_" + (j+row)) * 1000) / 1000);
            }
        }
        return Matrix;
    }

    private Map<String, Double> randomRes(Map<String, Double> matrix, int row, int col) {
        Map<String, Double> res = new HashMap<>();
        Map<String, Double> mat = new HashMap<>();
        int min = (int) Math.ceil(10/col);
        int max = (int) Math.ceil(100/min);
        int mat_col = (int) (Math.random()*++max)+min;
        res.put("mat_col", (double) mat_col);
        int notNullCount = 0;
        ArrayList<Integer> fullNull = new ArrayList<>();
        int colNull;
        for (int j = 1; j <=mat_col; j++) {
            colNull = 0;
            for (int i=1; i<=col; i++) {
                if (Math.random()<0.65) {
                    mat.put("m_" + i + "_" + j, 0.0);
                    colNull++;
                }
                else {
                    if ((double) notNullCount/mat_col/col<0.4) {
                        mat.put("m_" + i + "_" + j, (double) (int) (Math.random()*100)+1);
                        notNullCount++;
                    } else {
                        mat.put("m_" + i + "_" + j, 0.0);
                        colNull++;
                    }
                }
            }
            if (colNull==mat_col) fullNull.add(j);
        }
        for (int i=1; i<=row; i++) {
            for (int j = 1; j <= mat_col; j++) {
                if (fullNull.contains(j)) for (int k=1; k<=col; k++) res.put("m_"+i+"_"+j, 0.0);
                else for (int k=1; k<=col; k++) {
                    if (res.get("m_"+i+"_"+j)!=null)
                        res.put("m_"+i+"_"+j, res.get("m_"+i+"_"+j)+matrix.get("m_"+i+"_"+k)*mat.get("m_"+k+"_"+j));
                    else res.put("m_"+i+"_"+j, matrix.get("m_"+i+"_"+k)*mat.get("m_"+k+"_"+j));
                }
            }
        }
        return res;
    }

    private Map<String, Double> staticRes(String fileName, Map<String, Double> matrix, int row, int col) throws IOException {
        Map<String, Double> res = new HashMap<>();
        FileReader fr= new FileReader("matrices/"+fileName);
        Scanner scan = new Scanner(fr);
        String line;
        ArrayList<ArrayList<Double>> mat = new ArrayList<>();
        int mat_col = 0, prev_col = 0;
        while (scan.hasNextLine()) {
            mat_col = 0;
            line = scan.nextLine();
            ArrayList<Double> mat_row = new ArrayList<>();
            for (String field : line.split(";")) {
                mat_row.add(Double.parseDouble(field));
                mat_col++;
            }
            if (prev_col==0) {
                prev_col = mat_col;
            }
            if (mat_col!=prev_col) {
                res.put("wrong", 1.0);
                fr.close();
                return res;
            }
            mat.add(mat_row);
        }
        fr.close();
        res.put("col", (double) prev_col);
        if (col!=mat.size()) {
            res.put("wrong", 0.0);
            return res;
        }
        for (int i=1; i<=row; i++) {
            for (int j = 1; j <= mat_col; j++) {
                for (int k=1; k<=col; k++) {
                    if (res.get("m_"+i+"_"+j)!=null)
                        res.put("m_"+i+"_"+j, res.get("m_"+i+"_"+j)+matrix.get("m_"+i+"_"+k)*mat.get(k-1).get(j-1));
                    else res.put("m_"+i+"_"+j, matrix.get("m_"+i+"_"+k)*mat.get(k-1).get(j-1));
                }
            }
        }
        return res;
    }

    private void outResult(Map<String, Double> res, int row, int col) throws IOException {
        SimpleDateFormat dF = new SimpleDateFormat("yy_MM_dd HH_mm_ss");
        String fileName = dF.format(new Date())+".csv";

        Path dir = Files.createDirectories(Paths.get("results"));
        OutputStream out = Files.newOutputStream(dir.resolve(fileName));
        out.close();

        FileWriter fw = new FileWriter("results\\"+fileName);
        String line;
        for (int i=1; i<=row; i++) {
            line = "";
            for (int j=1; j<=col; j++) {
                line=line+res.get("m_"+i+"_"+j).toString()+";";
                if (j==col) {
                    line = line.substring(0, line.length()-1);
                }
            }
            fw.write(line+"\n");
        }
        fw.flush();
        fw.close();
    }
}