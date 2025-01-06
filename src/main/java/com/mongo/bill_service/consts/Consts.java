package com.mongo.bill_service.consts;

import java.util.List;
import java.util.stream.Collectors;

import com.mongo.bill_service.entities.Split;
import com.mongo.bill_service.entities.SplitResponse;

public class Consts {

	public static final String welcomeHTML = """
			<!DOCTYPE html>
			<html lang="en">
			<head>
			    <meta charset="UTF-8">
			    <meta name="viewport" content="width=device-width, initial-scale=1.0">
			    <title>Expense Split Details</title>
			    <!-- Bootstrap CSS -->
			    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0-alpha1/dist/css/bootstrap.min.css" rel="stylesheet">
			    <style>
			        body {
			            background-color: #f8f9fa;
			        }
			        .card {
			            border-radius: 15px;
			            box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
			        }
			        .card-header {
			            font-size: 1.25rem;
			            font-weight: bold;
			            background-color: #007bff;
			            color: white;
			            text-align: center;
			        }
			        .card-body {
			            background-color: #ffffff;
			        }
			        .item-list {
			            list-style-type: none;
			            padding-left: 0;
			        }
			        .item-list li {
			            padding: 5px 0;
			        }
			        .total-section {
			            font-size: 1.5rem;
			            font-weight: bold;
			            margin-top: 20px;
			        }
			    </style>
			</head>
			<body>
			    <div class="container">
			        <h1 class="text-center my-4">Expense Split Details</h1>
			        <div class="row row-cols-1 row-cols-md-2 row-cols-lg-4 g-4">
			         	%s
			        </div>
			        <div class="text-center total-section">
			            Total: ₹%.2f
			        </div>
			    </div>

			    <!-- Bootstrap JS -->
			    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0-alpha1/dist/js/bootstrap.bundle.min.js"></script>
			</body>
			</html>

				""";

	public static String getSplitHTML(SplitResponse splitResponse) {

		String cards = splitResponse.getDetails().stream().map(t -> getCard(t)).collect(Collectors.joining("\n"));

		return String.format(welcomeHTML, cards, splitResponse.getTotal());
	}

	public static String getCard(Split split) {

		String template = """

				<div class="col">
				             <div class="card">
				                 <div class="card-header">%s</div>
				                 <div class="card-body">
				                     <p><strong>Split Amount: ₹%.2f</strong> </p>
				                     <p><strong>Items: %d</strong></p>
				                     %s
				                 </div>
				             </div>
				       </div>


				""";

		return String.format(template, split.getName(), split.getSplit(), split.getItemcount());
	}

	public static String getUl(List<String> item) {

		String uls = """

				<ul class="item-list">
				          %s
				       </ul>

				""";

		return String.format(uls, item.stream().map(t -> "<li>" + t + "</li>").collect(Collectors.joining("\n")));

	}
}
