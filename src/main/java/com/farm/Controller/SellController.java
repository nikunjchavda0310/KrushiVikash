package com.farm.Controller;

import com.farm.Entity.Farmer;
import com.farm.Entity.OrderItem;
import com.farm.Repository.FarmerRepository;
import com.farm.Repository.OrderItemRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/farmer")
public class SellController {
    @Autowired
    private FarmerRepository farmerRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @GetMapping("/total-sell")
    public String getTotalSell(
            @RequestParam(value = "filter", defaultValue = "all") String filter,
            Model model, Principal principal, HttpServletRequest request) {

        String email = principal.getName();
        Optional<Farmer> farmerOpt = farmerRepository.findByEmail(email);

        if (farmerOpt.isEmpty()) return "redirect:/login";
        Farmer farmer = farmerOpt.get();

        List<OrderItem> allItems = orderItemRepository.findByProductFarmerId(farmer.getId());

        LocalDateTime now = LocalDateTime.now();
        List<OrderItem> filteredItems = allItems.stream().filter(item -> {
            LocalDateTime orderDate = item.getOrder().getOrderDate();
            switch (filter) {
                case "today": return orderDate.toLocalDate().isEqual(now.toLocalDate());
                case "month": return orderDate.getMonth() == now.getMonth() && orderDate.getYear() == now.getYear();
                case "year": return orderDate.getYear() == now.getYear();
                default: return true;
            }
        }).collect(Collectors.toList());

        // Calculate Summary Stats
        double totalRevenue = filteredItems.stream()
                .mapToDouble(item -> item.getPriceAtPurchase() * item.getQuantity())
                .sum();

        int totalQty = filteredItems.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();

        // Grouping logic for charts
        Map<String, Map<String, Integer>> categoryData = filteredItems.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getProduct().getCategory().getName(),
                        Collectors.groupingBy(
                                item -> item.getProduct().getProductName(),
                                Collectors.summingInt(OrderItem::getQuantity)
                        )
                ));

        model.addAttribute("farmer", farmer);
        model.addAttribute("categoryData", categoryData);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalQty", totalQty);
        model.addAttribute("currentFilter", filter);

        // Check if the request is AJAX (from your loadContent function)
        String requestedWith = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equals(requestedWith)) {
            // Return only the inner content fragment
            return "farmer/total-sell :: salesContent";
        }

        // Return the full page for regular browser refreshes
        return "farmer/total-sell";
    }

    @GetMapping("/export-sales")
    public void exportToExcel(@RequestParam(value = "filter", defaultValue = "all") String filter,
                              HttpServletResponse response, Principal principal) throws IOException {

        String email = principal.getName();
        Optional<Farmer> farmer = farmerRepository.findByEmail(email);
        List<OrderItem> allItems = orderItemRepository.findByProductFarmerId(farmer.get().getId());

        // 1. Apply the same date filter
        LocalDateTime now = LocalDateTime.now();
        List<OrderItem> filteredItems = allItems.stream().filter(item -> {
            LocalDateTime d = item.getOrder().getOrderDate();
            switch (filter) {
                case "today": return d.toLocalDate().isEqual(now.toLocalDate());
                case "month": return d.getMonth() == now.getMonth() && d.getYear() == now.getYear();
                case "year": return d.getYear() == now.getYear();
                default: return true;
            }
        }).collect(Collectors.toList());

        // 2. Setup Excel Workbook
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sales Report");
            Row header = sheet.createRow(0);
            String[] columns = {"Order ID", "Date", "Category", "Product", "Qty", "Price", "Total"};

            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            // 3. Fill Data
            int rowIdx = 1;
            for (OrderItem item : filteredItems) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(item.getOrder().getId());
                row.createCell(1).setCellValue(item.getOrder().getOrderDate().toString());
                row.createCell(2).setCellValue(item.getProduct().getCategory().getName());
                row.createCell(3).setCellValue(item.getProduct().getProductName());
                row.createCell(4).setCellValue(item.getQuantity());
                row.createCell(5).setCellValue(item.getPriceAtPurchase());
                row.createCell(6).setCellValue(item.getQuantity() * item.getPriceAtPurchase());
            }

            // 4. Set Response Headers and Download
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=Sales_Report_" + filter + ".xlsx");
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
