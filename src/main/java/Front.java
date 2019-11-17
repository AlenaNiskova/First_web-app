import javax.servlet.ServletException;
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
import java.util.stream.Stream;

public class Front extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        switch (request.getRequestURI()) {
            case ("/"): {
                response.getWriter().println(PageGenerator.instance().getPage("input.html", matrixVars(request)));
                response.setStatus(HttpServletResponse.SC_OK);
                break;
            }
            case ("/multiplication"): {
                createPage(response, request, matrixVars(request), "multiplication.html");
                break;
            }
            case ("/gaussian"): {
                createPage(response, request, matrixVars(request), "gaussian.html");
                break;
            }
            default: {
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().println("<a href=\"http://localhost:8080/\">На главную</a>");
                break;
            }
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private static Map<String, Object> matrixVars(HttpServletRequest request) {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("matrix_row", request.getParameter("matrix_row"));
        vars.put("matrix_col", request.getParameter("matrix_col"));
        return vars;
    }

    private List<File> getFiles (HttpServletRequest request, HttpServletResponse response, String file, Map<String, Object> vars) throws IOException {
        List<File> files = new LinkedList<>();
        response.getWriter().println(PageGenerator.instance().getPage(file, vars));
        if (!request.getRequestURI().equals("/gaussian")) {
            response.getWriter().println("<select id=\"select\" name=\"file_matrix\"><option disabled>Matrix_row_col</option>");
            files = Files.walk(Paths.get("matrices"))
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .collect(Collectors.toList());
            String selected = request.getParameter("file_matrix");
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
        }
        response.getWriter().println("<input type=\"submit\" value=\"Отправить\"><br/></form>");
        return files;
    }

    private void createPage(HttpServletResponse response, HttpServletRequest request, Map<String, Object> vars, String file) throws IOException {
        List<File> files = new LinkedList<>();
        if (vars.get("matrix_row") == null) {
            vars.put("matrix_row", 0);
            vars.put("matrix_col", 0);
            files = getFiles(request, response, file, vars);
        }
        else {
            files = getFiles(request, response, file, vars);
            String selected = request.getParameter("file_matrix");
            if (request.getParameter("which_matrix")!=null && request.getParameter("which_matrix").equals("static")) {
                if (selected.matches("matrix_\\d+_\\d+[.]csv")) {
                    selected = selected.substring(0, selected.length() - 4);
                    String[] matr = selected.split("_");
                    if (Integer.parseInt(matr[1]) != Integer.parseInt(vars.get("matrix_col").toString())) {
                        response.getWriter().println("Количество столбцов вашей матрицы не совпадает с количеством строк матрицы в файле, умножение не может быть произведено.");
                        return;
                    }
                } else {
                    response.getWriter().println("Имя файла не соответствует шаблону matrix_[0-9]+_[0-9]+.csv, где первое число - количество строк, второе - количество столбцов.<br/>" +
                            "Пожалуйста, переименуйте файл, обновите страницу и выберите новый файл во избежание несовпадения количества столбцов вашей матрицы с матрицей в файле.<br/> При несовпадении умножение невозможно.");
                }
            }

            response.getWriter().println("<form method=\"post\" name=\"matrix\">");
            if (file.equals("multiplication.html")) {
                response.getWriter().println("<input type=\"hidden\" name=\"which\" value=\""+request.getParameter("which_matrix")+"\">");
                response.getWriter().println("<input type=\"hidden\" name=\"file\" value=\""+request.getParameter("file_matrix")+"\">");
            }
            response.getWriter().println("<input type=\"hidden\" name=\"mat_row\" value=\""+vars.get("matrix_row").toString()+"\">");
            if (!file.equals("gaussian.html"))
                response.getWriter().println("<input type=\"hidden\" name=\"mat_col\" value=\""+vars.get("matrix_col").toString()+"\">");
            response.getWriter().println("<table>");
            for (int i=1; i<=(int) Integer.parseInt(vars.get("matrix_row").toString()); i++) {
                response.getWriter().println("<tr>");
                if (file.equals("gaussian.html")) {
                    for (int j = 1; j <= (int) Integer.parseInt(vars.get("matrix_row").toString()); j++) {
                        response.getWriter().println("<td><input style=\"width: 60px;\" max=\"100\" type=\"number\" name=\"m_" + i + "_" + j + "\"></td>");
                    }
                } else {
                    for (int j = 1; j <= (int) Integer.parseInt(vars.get("matrix_col").toString()); j++) {
                        response.getWriter().println("<td><input style=\"width: 60px;\" max=\"100\" type=\"number\" name=\"m_" + i + "_" + j + "\"></td>");
                    }
                }
                response.getWriter().println("</tr>");
            }
            response.getWriter().println("</table><input type=\"submit\" value=\"Произвести действие\"></form>");
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.getParameter("matrix_row");
        response.setContentType("text/html;charset=UTF-8");
        switch (request.getRequestURI()) {
            case ("/multiplication"): {
                int row = Integer.parseInt(request.getParameter("mat_row"));
                int col = Integer.parseInt(request.getParameter("mat_col"));
                Map<String, Integer> matrix = returnMatrix(request, row, col);
                String which = request.getParameter("which");
                Map<String, Double> mat = new HashMap<>();
                Map<String, Double> res = new HashMap<>();
                switch (which) {
                    case ("unit"): {
                        //При умножении матрицы на единичную матрицу всегда получится та же матрица
                        outResult(matrix, row, col);
                        createPage(response, request, matrixVars(request), "multiplication.html");
                        response.getWriter().println("Успешно");
                        break;
                    }
                    case ("random"): {
                        int mat_row = col;
                        int min = (int) Math.ceil(10/col);
                        int max = (int) Math.ceil(100/min);
                        int mat_col = (int) (Math.random()*++max)+min;
                        int notNullCount = 0;
                        boolean a = false;
                        ArrayList<Integer> fullNull = new ArrayList<>();
                        int colNull;
                        for (int j = 1; j <= mat_col; j++) {
                            colNull = 0;
                            for (int i=1; i<=mat_row; i++) {
                                if (Math.random()<0.65) {
                                    mat.put("m_" + i + "_" + j, 0.0);
                                    colNull++;
                                }
                                else {
                                    if ((double) notNullCount/mat_col/mat_row<0.4) {
                                        mat.put("m_" + i + "_" + j, (double) (int) (Math.random()*100)+1);
                                        notNullCount++;
                                    } else {
                                        mat.put("m_" + i + "_" + j, 0.0);
                                        colNull++;
                                        a=true;
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
                        outResult(res, row, mat_col);
                        createPage(response, request, matrixVars(request), "multiplication.html");
                        response.getWriter().println("Успешно");
                        break;
                    }
                    case ("static"): {
                        String selected = request.getParameter("file");
                        String[] matr = selected.substring(0, selected.length()-4).split("_");
                        res = staticRes(selected, matrix, row, col);
                        if (res.containsKey("wrong")) {
                            createPage(response, request, matrixVars(request), "multiplication.html");
                            response.getWriter().println("Размерность матрицы в названии файла не совпадает с действительностью, умножение не может быть произведено");
                        }
                        else {
                            outResult(res, row, Integer.parseInt(matr[2]));
                            createPage(response, request, matrixVars(request), "multiplication.html");
                            response.getWriter().println("Успешно");
                        }
                        break;
                    }
                }
                response.setContentType("text/html;charset=UTF-8");
                response.setStatus(HttpServletResponse.SC_OK);
                break;
            }
            case ("/gaussian"): {
                int row = Integer.parseInt(request.getParameter("mat_row"));
                Map<String, Integer> matrix = returnMatrix(request, row, row);
                Map<String, Double> res = gaussianRes(matrix, row, row);
                if (res.containsKey("wrong")) {
                    createPage(response, request, matrixVars(request), "gaussian.html");
                    response.getWriter().println("Не существует обратной матрицы.");
                }
                else {
                    outResult(res, row, row);
                    createPage(response, request, matrixVars(request), "gaussian.html");
                    response.getWriter().println("Успешно");
                }
                break;
            }
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private Map<String, Double> staticRes(String fileName, Map<String, Integer> matrix, int row, int col) throws IOException, FileNotFoundException {
        Map<String, Double> res = new HashMap<>();
        FileReader fr= new FileReader("matrices/"+fileName);
        Scanner scan = new Scanner(fr);
        String line;
        ArrayList<ArrayList<Double>> mat = new ArrayList<>();
        int mat_col = 0;
        while (scan.hasNextLine()) {
            line = scan.nextLine();
            ArrayList<Double> mat_row = new ArrayList<>();
            if (mat_col==0)
                for (String field: line.split(";")) {
                    mat_row.add(Double.parseDouble(field));
                    mat_col++;
                }
            else for (String field: line.split(";"))
                mat_row.add(Double.parseDouble(field));
            mat.add(mat_row);
        }
        fr.close();
        if (col!=mat.size()) {
            res.put("wrong", 1.0);
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

    private Map<String, Double> gaussianRes(Map<String, Integer> matrix, int row, int col) {
        Map<String, Double> matrixBig = new HashMap<>();
        Map<String, Double> Matrix = new HashMap<>();
        for (int i=0; i<row; i++) {
            for (int j=0; j<row; j++) {
                matrixBig.put("m_"+i+"_"+j, (double) matrix.get("m_"+(i+1)+"_"+(j+1)));
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
        boolean wrong = false; Double present;
        for (int i = 0; i < row; i++) {
            if (wrong) {
                Matrix.put("wrong", 1.0);
                break;
            }
            for (int j = 0; j < row; j++) {
                present = matrixBig.get("m_" + i + "_" + (j+row));
                if (present.isInfinite() || present.isNaN()) {
                    wrong = true;
                    break;
                }
                Matrix.put("m_" + (i+1) + "_" + (j+1), (double) Math.round(matrixBig.get("m_" + i + "_" + (j+row)) * 1000) / 1000);
            }
        }
        return Matrix;
    }

    private void outResult(Map<String, ?> res, int row, int col) throws IOException {
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

    private static Map<String, Integer> returnMatrix(HttpServletRequest request, int row, int col) {
        Map<String, Integer> matrix = new HashMap<>();
        for (int i=1; i<=row; i++) {
            for (int j=1; j<=col; j++) {
                matrix.put("m_"+i+"_"+j, Integer.parseInt(request.getParameter("m_"+i+"_"+j).toString()));
            }
        }
        return matrix;
    }
}